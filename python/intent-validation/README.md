## 项目简介

本项目是一个基于大语言模型的意图分类验证系统，使用通义千问（Qwen）API 对用户输入的文本进行意图识别，并在 Amazon MASSIVE 中文意图数据集上进行验证评估。

## 功能特性

- 使用通义千问 API 进行意图分类
- 支持 60 种不同的意图类别识别
- 基于 Amazon MASSIVE 中文意图数据集进行验证
- 自动生成准确率和详细分类指标报告
- 支持自定义测试样本数量

## 环境要求

- Python 3.10
- 虚拟环境（.venv）

## 安装步骤

1. 创建并激活虚拟环境

```bash
python -m venv .venv
source .venv/Scripts/activate  # Windows Git Bash
# 或
.venv\Scripts\activate  # Windows CMD
```

2. 安装依赖包

```bash
pip install dashscope datasets scikit-learn tqdm
```

或使用代理安装

```bash
pip install --proxy http://127.0.0.1:7890 dashscope datasets scikit-learn tqdm
```

### 核心依赖说明

- **dashscope**: 通义千问 API SDK，用于调用大语言模型进行意图分类
- **datasets**: Hugging Face 数据集库，用于加载 Amazon MASSIVE 意图数据集
- **scikit-learn**: 机器学习库，用于计算准确率和生成分类报告
- **tqdm**: 进度条库，显示测试进度
- **numpy**: 数值计算库
- **pandas**: 数据处理库

## 使用方法

1. 配置 API 密钥

在 `main.py` 文件中修改 `API_KEY` 变量为你的通义千问 API 密钥：

```python
API_KEY = "your-api-key-here"
```

2. 运行验证

```bash
python main.py
```

默认会测试 1000 个样本，如需修改测试数量，可在 `main.py` 的 `run_evaluation()` 函数调用中修改 `num_samples` 参数。

3. 查看结果

程序运行完成后会输出：

- 整体准确率（Accuracy）
- 每个意图类别的详细分类指标（精确率、召回率、F1 分数）
