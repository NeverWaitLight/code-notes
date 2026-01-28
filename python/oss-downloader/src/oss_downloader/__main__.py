try:
    # 当作为包执行（python -m oss_downloader）时
    from .cli import main
except ImportError:
    # 当被 PyInstaller 当作脚本执行时
    from oss_downloader.cli import main


if __name__ == "__main__":
    main()
