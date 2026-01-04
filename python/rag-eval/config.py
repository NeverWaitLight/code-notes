"""
配置管理模块
管理API密钥和端点配置
"""


class Config:
    """配置类，管理阿里百炼DashScope API配置"""

    # API密钥（请在此处填写你的API密钥）
    DASHSCOPE_API_KEY = "sk-a4e10b09167d48f686eca678dedd4dd6"

    # 阿里百炼DashScope端点
    DASHSCOPE_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1"

    # 模型名称
    MODEL_NAME = "qwen3-max"

    # Embedding模型名称
    EMBEDDING_MODEL_NAME = "text-embedding-v4"

    # CommonRAG接口配置
    RAG_BASE_URL = "http://192.168.101.66:8013"
    RAG_PROJECT_ID = "chat_2005845271228973056"
    RAG_TIMEOUT = 99999999999999999999.0
    RAG_TOP_K = 10
    RAG_USE_RANKER = False
    RAG_SCORE_THRESHOLD = None

    @classmethod
    def get_api_key(cls) -> str:
        """
        获取API密钥
        从常量中读取
        """
        return cls.DASHSCOPE_API_KEY

    @classmethod
    def get_base_url(cls) -> str:
        """获取API基础URL"""
        return cls.DASHSCOPE_BASE_URL

    @classmethod
    def get_model_name(cls) -> str:
        """获取模型名称"""
        return cls.MODEL_NAME

    @classmethod
    def get_embedding_model_name(cls) -> str:
        """获取Embedding模型名称"""
        return cls.EMBEDDING_MODEL_NAME

    @classmethod
    def validate_config(cls) -> bool:
        """
        验证配置是否完整
        返回True如果配置有效，否则返回False
        """
        api_key = cls.get_api_key()
        if not api_key:
            print("警告: 未设置DASHSCOPE_API_KEY，请在config.py中设置常量")
            return False
        return True
