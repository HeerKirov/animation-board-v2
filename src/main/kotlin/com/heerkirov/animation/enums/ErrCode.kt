package com.heerkirov.animation.enums

enum class ErrCode {
    UNAUTHORIZED,           //401 无认证信息
    AUTHENTICATION_FAILED,  //401 无效的认证信息
    FORBIDDEN,              //403 权限不足

    EMPTY_REQUEST_BODY,     //400 请求体为空
    INVALID_REQUEST_BODY,   //400 请求体解析失败
    INVALID_CONTENT_TYPE,   //400 错误的content-type
    TYPE_ERROR,             //400 错误的解析参数格式
    PARAM_ERROR,            //400 错误的参数
    PARAM_REQUIRED,         //400 必选参数
    INVALID_OPERATION,      //400 无效的操作
    ALREADY_EXISTS,         //400 已经存在的资源
    NOT_EXISTS,             //400 找不到资源
    NOT_FOUND,              //404 找不到资源
    INTERNAL_SERVER_ERROR   //500 内部错误
}