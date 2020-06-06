package com.heerkirov.animation.util.transform

import com.heerkirov.animation.dao.Users
import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.data.UserSetting
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Tips:
 * - V1的数据库都是带时区的时间，经过ktorm读取之后的LocalDateTime都是本地时区而非UTC时区的时间。写到V2数据库时需要做转换。
 */
fun transformV1ToV2(v1Database: Database, database: Database) {
    val userLoader = UserLoader(v1Database, database)

    database.useTransaction {
        val animationIdMap = TransformAnimation(userLoader, v1Database, database).transform()

        TransformTag(userLoader, v1Database, database).transform(animationIdMap)

        TransformStaff(userLoader, v1Database, database).transform(animationIdMap)

        TransformComment(userLoader, v1Database, database).transform(animationIdMap)

        TransformDiary(userLoader, v1Database, database).transform(animationIdMap)
    }
}

/**
 * 在v1中用户以username表示，ownerId联系的用户也需要查询username。此类在v2中缓存对不同username的user的查询。
 */
class UserLoader(private val v1Database: Database, private val database: Database) {
    private val usernameCache = HashMap<String, User>()
    private val idCache = HashMap<Int, User>()

    operator fun get(userId: Int): User {
        val item = idCache[userId]
        if(item == null) {
            val username = v1Database.from(V1Profiles)
                    .select(V1Profiles.username)
                    .where { V1Profiles.id eq userId }.first()[V1Profiles.username]!!
            val user = get(username)
            idCache[userId] = user
            return user
        }
        return item
    }

    operator fun get(username: String): User {
        val item = usernameCache[username]
        if(item == null) {
            val user = database.from(Users).select()
                    .where { Users.username eq username }
                    .firstOrNull()
                    ?.let { Users.createEntity(it) }
                    ?: createUser(username)
            usernameCache[username] = user
            return user
        }
        return item
    }

    private fun createUser(username: String): User {
        val v1User = v1Database.from(V1Profiles)
                .innerJoin(V1Users, V1Users.id eq V1Profiles.userId)
                .select(V1Profiles.nightUpdateMode, V1Profiles.animationUpdateNotice, V1Users.isStaff)
                .where { V1Profiles.username eq username }.first()

        val userSetting = UserSetting(
                animationUpdateNotice = v1User[V1Profiles.animationUpdateNotice]!!,
                nightTimeTable = v1User[V1Profiles.nightUpdateMode]!!
        )
        val isStaff = v1User[V1Users.isStaff]!!
        val id = database.insertAndGenerateKey(Users) {
            it.username to username
            it.isStaff to isStaff
            it.setting to userSetting
        }
        return User(id as Int, username, isStaff, userSetting)
    }
}

/**
 * 由于v1数据库的时间都带时区，从ktorm读进来后都是基于本地时区的时间。这个函数将此时间转换为UTC时区的时间。
 */
fun LocalDateTime.toV2Time(): LocalDateTime {
    return this.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime()
}