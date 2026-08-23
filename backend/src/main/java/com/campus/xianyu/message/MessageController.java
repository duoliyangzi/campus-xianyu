package com.campus.xianyu.message;



import com.campus.xianyu.auth.TokenService;

import com.campus.xianyu.common.ApiResponse;

import com.campus.xianyu.product.ProductRepository;

import com.campus.xianyu.user.AppUser;

import com.campus.xianyu.user.PublicUserResponse;

import com.campus.xianyu.user.UserRepository;

import com.campus.xianyu.wanted.WantedRepository;

import jakarta.validation.Valid;

import java.time.LocalDateTime;

import java.util.ArrayList;

import java.util.Comparator;

import java.util.LinkedHashMap;

import java.util.List;

import java.util.Map;

import java.util.function.Function;

import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.web.bind.annotation.RequestHeader;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;



@RestController

@RequestMapping("/api/messages")

public class MessageController {

    private final TokenService tokenService;

    private final UserRepository userRepository;

    private final ProductRepository productRepository;

    private final WantedRepository wantedRepository;

    private final ConversationRepository conversationRepository;

    private final ChatMessageRepository chatMessageRepository;



    public MessageController(

            TokenService tokenService,

            UserRepository userRepository,

            ProductRepository productRepository,

            WantedRepository wantedRepository,

            ConversationRepository conversationRepository,

            ChatMessageRepository chatMessageRepository

    ) {

        this.tokenService = tokenService;

        this.userRepository = userRepository;

        this.productRepository = productRepository;

        this.wantedRepository = wantedRepository;

        this.conversationRepository = conversationRepository;

        this.chatMessageRepository = chatMessageRepository;

    }



    @PostMapping("/conversations")

    @Transactional

    public ApiResponse<ConversationResponse> createConversation(

            @RequestHeader(value = "Authorization", required = false) String authorization,

            @Valid @RequestBody ConversationCreateRequest request

    ) {

        AppUser user = requireLogin(authorization);

        if (request.peerUserId().equals(user.getId())) {

            throw new IllegalArgumentException("不能和自己发起私聊");

        }

        userRepository.findById(request.peerUserId())

                .orElseThrow(() -> new IllegalArgumentException("对方用户不存在"));

        if (request.productId() == null && request.wantedId() == null) {

            throw new IllegalArgumentException("请关联商品或求购");

        }

        if (request.productId() != null) {

            productRepository.findById(request.productId())

                    .orElseThrow(() -> new IllegalArgumentException("商品不存在"));

        }

        if (request.wantedId() != null) {

            wantedRepository.findById(request.wantedId())

                    .orElseThrow(() -> new IllegalArgumentException("求购不存在"));

        }



        long userAId = Math.min(user.getId(), request.peerUserId());

        long userBId = Math.max(user.getId(), request.peerUserId());

        List<Conversation> existing = conversationRepository.findByUserAIdAndUserBId(userAId, userBId);

        Conversation conversation = existing.stream()

                .max(Comparator

                        .comparing(Conversation::getLastMsgAt, Comparator.nullsLast(Comparator.naturalOrder()))

                        .thenComparing(Conversation::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))

                .orElseGet(() -> {

                    Conversation created = new Conversation();

                    created.setUserAId(userAId);

                    created.setUserBId(userBId);

                    created.setProductId(request.productId());

                    created.setWantedId(request.wantedId());

                    return conversationRepository.save(created);

                });



        AppUser peer = userRepository.findById(request.peerUserId()).orElseThrow();

        String lastMessage = latestMessage(conversationIdsBetween(user.getId(), request.peerUserId()));

        int unreadCount = unreadCountBetween(user.getId(), request.peerUserId());

        return ApiResponse.ok(toConversationResponse(conversation, user.getId(), peer, lastMessage, unreadCount));

    }



    @GetMapping("/conversations")

    public ApiResponse<List<ConversationResponse>> listConversations(

            @RequestHeader(value = "Authorization", required = false) String authorization

    ) {

        AppUser user = requireLogin(authorization);

        List<Conversation> conversations = conversationRepository

                .findByUserAIdOrUserBIdOrderByLastMsgAtDescCreatedAtDesc(user.getId(), user.getId());

        conversations = conversations.stream()

                .filter(conversation -> chatMessageRepository.existsByConversationId(conversation.getId()))

                .toList();



        Map<Long, List<Conversation>> grouped = new LinkedHashMap<>();

        for (Conversation conversation : conversations) {

            Long peerId = peerId(conversation, user.getId());

            grouped.computeIfAbsent(peerId, key -> new ArrayList<>()).add(conversation);

        }



        Map<Long, AppUser> users = userRepository.findAllById(grouped.keySet()).stream()

                .collect(Collectors.toMap(AppUser::getId, Function.identity()));



        List<ConversationResponse> responses = grouped.entrySet().stream()

                .map(entry -> {

                    Long peerId = entry.getKey();

                    List<Conversation> peerConversations = entry.getValue();

                    Conversation primary = peerConversations.stream()

                            .max(Comparator

                                    .comparing(Conversation::getLastMsgAt, Comparator.nullsLast(Comparator.naturalOrder()))

                                    .thenComparing(Conversation::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))

                            .orElse(peerConversations.get(0));

                    List<Long> conversationIds = peerConversations.stream().map(Conversation::getId).toList();

                    String lastMessage = latestMessage(conversationIds);

                    int unreadCount = conversationIds.stream()

                            .mapToInt(id -> (int) chatMessageRepository.countByConversationIdAndIsReadFalseAndSenderIdNot(id, user.getId()))

                            .sum();

                    return toConversationResponse(primary, user.getId(), users.get(peerId), lastMessage, unreadCount);

                })

                .sorted(Comparator

                        .comparing((ConversationResponse item) -> item.lastMsgAt(), Comparator.nullsLast(Comparator.reverseOrder()))

                        .thenComparing(item -> item.createdAt(), Comparator.nullsLast(Comparator.reverseOrder())))

                .toList();

        return ApiResponse.ok(responses);

    }



    @GetMapping("/conversations/{id}")

    @Transactional

    public ApiResponse<List<ChatMessageResponse>> getConversation(

            @RequestHeader(value = "Authorization", required = false) String authorization,

            @PathVariable Long id

    ) {

        AppUser user = requireLogin(authorization);

        Conversation conversation = requireParticipant(conversationRepository.findById(id)

                .orElseThrow(() -> new IllegalArgumentException("会话不存在")), user.getId());

        Long peerId = peerId(conversation, user.getId());

        List<Long> conversationIds = conversationIdsBetween(user.getId(), peerId);



        List<ChatMessage> messages = chatMessageRepository.findByConversationIdInOrderByCreatedAtAsc(conversationIds);

        Map<Long, AppUser> senders = userRepository.findAllById(

                messages.stream().map(ChatMessage::getSenderId).distinct().toList()

        ).stream().collect(Collectors.toMap(AppUser::getId, Function.identity()));



        for (ChatMessage message : messages) {

            if (!message.getSenderId().equals(user.getId()) && !Boolean.TRUE.equals(message.getIsRead())) {

                message.setIsRead(true);

            }

        }

        chatMessageRepository.saveAll(messages);



        List<ChatMessageResponse> responses = messages.stream()

                .map(message -> ChatMessageResponse.from(message, PublicUserResponse.from(senders.get(message.getSenderId()))))

                .toList();

        return ApiResponse.ok(responses);

    }



    @PostMapping("/conversations/{id}/messages")

    @Transactional

    public ApiResponse<ChatMessageResponse> sendMessage(

            @RequestHeader(value = "Authorization", required = false) String authorization,

            @PathVariable Long id,

            @Valid @RequestBody MessageSendRequest request

    ) {

        AppUser user = requireLogin(authorization);

        Conversation conversation = requireParticipant(conversationRepository.findById(id)

                .orElseThrow(() -> new IllegalArgumentException("会话不存在")), user.getId());

        Long peerId = peerId(conversation, user.getId());

        Conversation targetConversation = conversationRepository.findByUserAIdAndUserBId(

                        Math.min(user.getId(), peerId),

                        Math.max(user.getId(), peerId)

                ).stream()

                .max(Comparator

                        .comparing(Conversation::getLastMsgAt, Comparator.nullsLast(Comparator.naturalOrder()))

                        .thenComparing(Conversation::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))

                .orElse(conversation);



        ChatMessage message = new ChatMessage();

        message.setConversationId(targetConversation.getId());

        message.setSenderId(user.getId());

        message.setContent(request.content().trim());

        message.setIsRead(false);

        ChatMessage saved = chatMessageRepository.save(message);



        targetConversation.setLastMsgAt(LocalDateTime.now());

        conversationRepository.save(targetConversation);



        return ApiResponse.ok("发送成功", ChatMessageResponse.from(saved, PublicUserResponse.from(user)));

    }



    private List<Long> conversationIdsBetween(Long userId, Long peerId) {

        long userAId = Math.min(userId, peerId);

        long userBId = Math.max(userId, peerId);

        return conversationRepository.findByUserAIdAndUserBId(userAId, userBId).stream()

                .map(Conversation::getId)

                .toList();

    }



    private int unreadCountBetween(Long userId, Long peerId) {

        return conversationIdsBetween(userId, peerId).stream()

                .mapToInt(id -> (int) chatMessageRepository.countByConversationIdAndIsReadFalseAndSenderIdNot(id, userId))

                .sum();

    }



    private String latestMessage(List<Long> conversationIds) {

        if (conversationIds.isEmpty()) {

            return null;

        }

        return chatMessageRepository.findByConversationIdInOrderByCreatedAtAsc(conversationIds).stream()

                .reduce((first, second) -> second)

                .map(ChatMessage::getContent)

                .orElse(null);

    }



    private Long peerId(Conversation conversation, Long currentUserId) {

        return conversation.getUserAId().equals(currentUserId)

                ? conversation.getUserBId()

                : conversation.getUserAId();

    }



    private ConversationResponse toConversationResponse(

            Conversation conversation,

            Long currentUserId,

            AppUser peer,

            String lastMessage,

            int unreadCount

    ) {

        return ConversationResponse.from(

                conversation,

                currentUserId,

                peer == null ? null : PublicUserResponse.from(peer),

                lastMessage,

                unreadCount

        );

    }



    private Conversation requireParticipant(Conversation conversation, Long userId) {

        if (!conversation.getUserAId().equals(userId) && !conversation.getUserBId().equals(userId)) {

            throw new IllegalArgumentException("无权访问该会话");

        }

        return conversation;

    }



    private AppUser requireLogin(String authorization) {

        Long userId = tokenService.findUserId(authorization)

                .orElseThrow(() -> new IllegalArgumentException("请先登录"));

        return userRepository.findById(userId)

                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

    }

}


