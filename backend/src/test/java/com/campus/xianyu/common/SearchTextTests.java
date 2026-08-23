package com.campus.xianyu.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SearchTextTests {
    @Test
    void matchesChineseTextAndPinyinFragments() {
        assertThat(SearchText.matches("键盘", "九成新机械键盘")).isTrue();
        assertThat(SearchText.matches("jianpan", "九成新机械键盘")).isTrue();
        assertThat(SearchText.matches("xie", "九成新机械键盘")).isTrue();
        assertThat(SearchText.matches("jxjp", "九成新机械键盘")).isTrue();
    }

    @Test
    void matchesSellerNickname() {
        assertThat(SearchText.matches("xiaolin", "测试卖家小林")).isTrue();
        assertThat(SearchText.matches("小林", "测试卖家小林")).isTrue();
    }

    @Test
    void rejectsUnrelatedText() {
        assertThat(SearchText.matches("zixingche", "高等数学教材", "测试卖家小林")).isFalse();
    }

    @Test
    void exactAndDirectMatchesRankAbovePinyinMatches() {
        assertThat(SearchText.score("键盘", "键盘", 300))
                .isGreaterThan(SearchText.score("键盘", "九成新机械键盘", 300));
        assertThat(SearchText.score("jianpan", "九成新机械键盘", 300))
                .isGreaterThan(0);
        assertThat(SearchText.score("小林", "测试卖家小林", 200))
                .isGreaterThan(SearchText.score("小林", "描述里提到小林", 100));
    }
}
