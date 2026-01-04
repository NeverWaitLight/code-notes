"""
RAG客户端测试脚本
用于单独测试 rag_client.py 中的 RAGClient 功能
"""

import asyncio

from config import Config
from rag_client import RAGClient


async def test_basic_query():
    """测试基本查询功能"""
    print("=" * 60)
    print("测试1: 基本查询")
    print("=" * 60)
    
    client = RAGClient()
    user_input = "叶凡为什么被林婉清退婚"
    
    print(f"查询内容: {user_input}")
    print("配置信息:")
    print(f"  - RAG_BASE_URL: {Config.RAG_BASE_URL}")
    print(f"  - RAG_PROJECT_ID: {Config.RAG_PROJECT_ID}")
    print(f"  - RAG_TOP_K: {Config.RAG_TOP_K}")
    print(f"  - RAG_USE_RANKER: {Config.RAG_USE_RANKER}")
    print()
    
    try:
        result = await client.query(user_input)
        
        print("查询成功")
        print("-" * 60)
        print("检索到的上下文数量:", len(result["retrieved_contexts"]))
        print("-" * 60)
        
        if result["retrieved_contexts"]:
            print("检索到的上下文:")
            for i, ctx in enumerate(result["retrieved_contexts"], 1):
                print(f"\n上下文 {i}:")
                print(ctx[:200] + "..." if len(ctx) > 200 else ctx)
        else:
            print("未检索到上下文")
        
        print("-" * 60)
        print("生成的回答:")
        print(result["response"])
        print("=" * 60)
        
    except Exception as e:
        print(f"查询失败: {type(e).__name__}: {e}")
        print("=" * 60)


async def test_with_params():
    """测试带参数的查询"""
    print("\n" + "=" * 60)
    print("测试2: 带参数的查询")
    print("=" * 60)
    
    client = RAGClient()
    user_input = "仙火如何帮助叶凡提升修为"
    
    print(f"查询内容: {user_input}")
    print("查询参数:")
    print("  - type: test_type")
    print("  - top_k: 3")
    print("  - use_ranker: False")
    print()
    
    try:
        result = await client.query(
            user_input,
            type="test_type",
            top_k=3,
            use_ranker=False
        )
        
        print("查询成功")
        print("-" * 60)
        print("检索到的上下文数量:", len(result["retrieved_contexts"]))
        print("-" * 60)
        
        if result["retrieved_contexts"]:
            print("检索到的上下文:")
            for i, ctx in enumerate(result["retrieved_contexts"], 1):
                print(f"\n上下文 {i}:")
                print(ctx[:200] + "..." if len(ctx) > 200 else ctx)
        else:
            print("未检索到上下文")
        
        print("-" * 60)
        print("生成的回答:")
        print(result["response"])
        print("=" * 60)
        
    except Exception as e:
        print(f"查询失败: {type(e).__name__}: {e}")
        print("=" * 60)


async def test_custom_config():
    """测试自定义配置的客户端"""
    print("\n" + "=" * 60)
    print("测试3: 自定义配置的客户端")
    print("=" * 60)
    
    client = RAGClient(
        top_k=10,
        use_ranker=False,
        timeout=99999999.0
    )
    user_input = "青云门百峰试炼中发生了什么"
    
    print(f"查询内容: {user_input}")
    print("客户端配置:")
    print(f"  - top_k: {client.top_k}")
    print(f"  - use_ranker: {client.use_ranker}")
    print(f"  - timeout: {client.timeout}")
    print()
    
    try:
        result = await client.query(user_input)
        
        print("查询成功")
        print("-" * 60)
        print("检索到的上下文数量:", len(result["retrieved_contexts"]))
        print("-" * 60)
        
        if result["retrieved_contexts"]:
            print("检索到的上下文:")
            for i, ctx in enumerate(result["retrieved_contexts"], 1):
                print(f"\n上下文 {i}:")
                print(ctx[:200] + "..." if len(ctx) > 200 else ctx)
        else:
            print("未检索到上下文")
        
        print("-" * 60)
        print("生成的回答:")
        print(result["response"])
        print("=" * 60)
        
    except Exception as e:
        print(f"查询失败: {type(e).__name__}: {e}")
        print("=" * 60)


async def main():
    """主测试函数"""
    print("\n开始测试 RAGClient...\n")
    
    await test_basic_query()
    await test_with_params()
    await test_custom_config()
    
    print("\n所有测试完成")


if __name__ == "__main__":
    asyncio.run(main())

