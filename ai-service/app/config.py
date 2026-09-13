from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    ai_service_host: str = "0.0.0.0"
    ai_service_port: int = 8000
    llm_api_key: str = ""
    llm_base_url: str = "https://api.deepseek.com/v1"
    llm_model: str = "deepseek-chat"
    embedding_model: str = "text-embedding-v3"
    chroma_path: str = ".chroma"
    redis_url: str = ""
    rate_limit_per_minute: int = 20
    java_api_base: str = "http://127.0.0.1:8080/api"
    java_internal_token: str = ""
    knowledge_path: str = "knowledge/faq.md"
    chunk_size: int = 400
    chunk_overlap: int = 80
    log_level: str = "INFO"
    log_dir: str = "logs"


@lru_cache
def get_settings() -> Settings:
    return Settings()
