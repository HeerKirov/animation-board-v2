# Animation Board v2
动画数据库 & 观看记录与个人评价 & 行为统计项目

旧的[Animation Board](https://github.com/HeerKirov/AnimationBoard)项目的重构项目。重新设计了部分表和业务，精简API设计，并更换技术栈。

## Build
### 编译 & 打包
```bash
mvn clean package
```
### 部署
```bash
cp target/animation-board-v2-$VERSION.jar dist/animation-board-v2.jar
cp scripts/* dist/
cp src/main/resources/application.properties dist/

cd dist
vim application.properties     # 编辑配置文件

./server-start.sh              # 启动服务器
./server-stop.sh               # 关闭服务器

./transform-database.sh        # 将v1版本的数据库迁移到此项目
./transform-oss.sh             # 将v1版本的OSS文件迁移到此项目

```