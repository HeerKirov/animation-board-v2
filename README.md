# Animation Board v2
动画数据库 & 观看记录与个人评价 & 行为统计项目

旧的[Animation Board](https://github.com/HeerKirov/AnimationBoard)项目的重构项目。重新设计了部分表和业务，精简API设计，并更换技术栈。

## Technology Stack
* Kotlin
* Spring Boot
* Ktorm
* PostgreSQL
* Aliyun OSS

## Build
### Compile & Build
```bash
mvn clean package
```
### Deploy
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
### Configuration
`application.properties`的核心参数:
```properties
# postgres URL
spring.datasource.url=jdbc:postgresql://localhost:5432/animation_board_v2
# postgres username
spring.datasource.username=postgres
# postgres password
spring.datasource.password=

# BS服务URL
basic-service.url=http://basic-service
# 使用app id
basic-service.app-id=
# 使用app secret
basic-service.app-secret=

# OSS endpoint
oss.endpoint=https://oss-cn-beijing.aliyuncs.com
# OSS access-key id
oss.access-key.id=
# OSS access-key secret
oss.access-key.secret=
# OSS bucket name
oss.bucket-name=
# 从OSS取得的文件权限的缓存时间(ms)
oss.presigned-duration=3600000
```