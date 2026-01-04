[TOC]

# Git

## mono_release.sh

merge / deploy / upgrade tag

```shell
./mono_release.sh -f feat/task -r main
```

## release.sh

将 project-a \ project-c 的 feat/task 分支，将 project-b 的 feature/task 分支，合并到 main 分支并发布

脚本会把项目的 snapshot 版本修改为 release 版本并发布

```shell
./release.sh -fb feat/task -rb main -p project-a project-b:feature/task project-c
```

添加 `-r` 把 snapshot 升级为 release 版本

## delete_branch.sh

`delete_branch.sh` 清理 feat 开发分支、fix 修复分支

删除当前文件夹的 `feat/task` 分支

```shell
./delete_branch.sh -b feat/task
```

删除当前文件夹下的 **子文件** 的 `feat/task` 分支

```shell
./delete_branch.sh -b feat/task -r
```

删除当前文件夹下的 **子文件** 的 `fix` 开头的所有分支

```shell
./delete_branch.sh -b fix -r -a
```

# Java

## start.sh

启动（重启）了一个名为 service_name 的 jar ,指定了最大（小）内存为 4G

```shell
./start.sh service_name 4G
```

## startw.sh

启动（重启）了一个名为 service_name 的 jar ,指定了最大（小）内存为 4G，`startw.sh` 脚本会持续监控 jar 对应进程的 CPU 占用率，超过 90% 时自动执行 `jstack` 输出日志，直到 jar 对应的进程退出，`startw.sh` 才会退出

```shell
nohup ./startw.sh service_name 4G >/dev/null 2>&1 &
```

### 关闭监控

查找到 `startw.sh` 脚本的 `pid` 并关闭 `startw.sh` 对 CPU 占用的监控

```shell
ps -ef | grep -v grep | grep 'startw.sh service_name'
```

输出结果如下：

```shell
UID        PID  PPID  C STIME TTY          TIME CMD
username   1234 5678  0 10:00 pts/0    00:00:01 startw.sh service_name 4G
```

kill `startw.sh` 进程

```shell
kill -9 1234
```

## 简易 CI/CD

[deploy.sh](deploy.sh) 简易打包部署脚本

# 参数工具

## getopts

## argbash

---

# bin

1. 确保你的脚本有可执行权限。在 terminal（终端）里运行以下命令：

```shell
chmod +x /path/to/your/script.sh
```

2. 现在，你可以将脚本移动到 `/usr/local/bin` 或 `/usr/bin`

```shell
sudo mv /path/to/your/script.sh /usr/local/bin/
```

或者

```shell
sudo mv /path/to/your/script.sh /usr/bin/
```

3. 完成以上步骤后，你就可以在任何地方运行你的脚本了，只需像其他 bin 文件那样调用即可。

注意：如果你不希望移动原始脚本，也可以在 bin 目录中创建一个指向你脚本的软链接。使用以下命令：

```shell
sudo ln -s /path/to/your/script.sh /usr/local/bin/
```

或者

```shell
sudo ln -s /path/to/your/script.sh /usr/bin/
```
