"""Campus Xianyu MCP Server

把与 App 内 Function Calling 相同的能力通过 MCP 暴露给 Cursor / 兼容客户端。
启动：
  cd ai-service
  .\\.venv\\Scripts\\python.exe -m app.mcp_server
"""

import json

from mcp.server.fastmcp import FastMCP

from app.rag import get_rag
from app.tools import search_products

mcp = FastMCP("campus-xianyu")


@mcp.tool()
def search_campus_products(keyword: str) -> str:
    """在校园咸鱼首页搜索已发布在售商品。"""
    products = search_products((keyword or "").strip(), size=6)
    return json.dumps(
        {"keyword": keyword, "count": len(products), "products": products},
        ensure_ascii=False,
    )


@mcp.tool()
def retrieve_faq(query: str) -> str:
    """检索校园咸鱼 FAQ 知识库。"""
    hits = get_rag().retrieve((query or "").strip(), top_k=3)
    citations = [
        {"title": h.title, "content": h.content, "score": round(h.score, 4)} for h in hits
    ]
    return json.dumps({"hit_count": len(citations), "citations": citations}, ensure_ascii=False)


@mcp.tool()
def risk_precheck(title: str, description: str = "") -> str:
    """对商品标题与描述做风控预审（含辱骂检测）。"""
    from app.agent_tools import risk_precheck as _risk

    return _risk.invoke({"title": title, "description": description})


if __name__ == "__main__":
    mcp.run()
