# EasyCode

## 介绍

该项目是为了方便日常开发，以 swagger definition language 为基础生成 SpringBoot项目的Controller、Service、FeignClient、Dto等对象。

- 减少文件创建、重复代码编写（spring mvc注解、dto的get set construct、feignclient的定义）的工作，减少了模板化代码开发的时间。
- 让开发人员注重于接口命名，接口参数设计，核心业务代码编码等工作。

## 使用须知

该插件应当作为辅助插件；可通过文件定义生成对应java class。生成后需检查是否是自己想要的结果；

## maven-plugin-sql-codegen

### 功能

目前实现了 mysql create table sql 转换 mybatis entity 。在这里也比较推荐使用mybatis plus这种半orm框架，不至于对sql生疏。

### 优点

研发人员平时改了数据库表结构，还需要同步项目里面的orm实体。容易遗漏，且浪费时间。用插件生成代码，项目里既能保留一份数据库schema文件又不用手写实体，改了数据库后导出 create table sql，直接用插件生成即可update

> **tips：**
>
> 开发中，select 全部用 mybatis 的xml实现，手写sql（慢sql，索引优化时好排查现有查询条件）。
>
> 除了简单的 selectById，deletebyId，updateById，其他也尽量用xml sql实现。

### 使用

全配置版

```xml
 <build>
        <plugins>
            <plugin>
                <groupId>com.easycode</groupId>
                <artifactId>maven-plugin-sql-codegen</artifactId>
                <version>1.0-SNAPSHOT</version>
                <configuration>
                    <!--                    注释了的配置都代表不是必须的-->
                    <config>
                        <!--                        <dbName>数据库类型（用于决定解析sql的方式），默认为: mysql</dbName>-->
                        <!--                        <ormName>持久化层类型（用于决定生成哪类实体），默认为: mybatis-plus</ormName>-->
                        <!--                        默认会在项目下找 resources/db/TableSchema.sql-->
                        <!--                        <sqlFilePath>/xx/xx/xxx.sql</sqlFilePath>-->

                        <!--                        默认为项目的 src/java/main,最后生成的entity文件则在 src/java/main/${basePackage}/entities/下 -->
                        <!--                        <srcJavaPath>/xx/yy/zz</srcJavaPath>-->
                        <!-- 指向灯塔项目 src/main/java-->
                        <!--                        <srcJavaPath>-->
                        <!--                            /Users/xx/code/src/main/java-->
                        <!--                        </srcJavaPath>-->
                        <basePackage>com.xxx.lighthouse</basePackage>
                        <!--                        <entityPackageName>实体包名,默认为 entities </entityPackageName>-->
                        <!--                        <entitySuffix>实体class名称后缀,默认为 DO</entitySuffix>-->
                        <mybatisPlusConfig>
                            <logicDelCols>
                                <item>
                                    <columnName>deleted_at</columnName>
                                    <deletedValue>now()</deletedValue>
                                    <notDeletedValue>1000-01-01 00:00:00</notDeletedValue>
                                </item>
                            </logicDelCols>
                            <!--   需要生成自动填充配置的字段    @TableField(value = "xxx", fill = FieldFill.INSERT)-->
                            <autoInsertFields>
                                <param>created_at</param>
                            </autoInsertFields>
                            <!--   需要生成自动填充配置的字段    @TableField(value = "xxx", fill = FieldFill.UPDATE)-->
                            <autoUpdateFields>
                                <param>updated_at</param>
                            </autoUpdateFields>
                            <!--   需要生成自动填充配置的字段    @TableField(value = "xxx", fill = FieldFill.INSERT_UPDATE)-->
                            <autoInsertOrUpdateFields>

                            </autoInsertOrUpdateFields>
                          
                          
                            <!--                            下列功能目前存在配置字段但不打算支持，考虑到文件覆盖的问题: mapper.xml和mapperInterface.java都是可能会有自定义的select方法，容易引起安全事故。。-->
                            <!--                            <enableOutputMapper>是否开启生成mapperInterface.java mapper.xml,默认 false</enableOutputMapper>-->
                            <!--                            <mapperInterfacePackage>mapper接口生成的包名,默认mappers</mapperInterfacePackage>-->
                            <!--                            <mapperXmlDirName>mapper.xml生成的目录名（相对于resources）,默认为 mapper</mapperXmlDirName>-->
                        </mybatisPlusConfig>
                    </config>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>SqlToEntityConvertor</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
```

简单配置版

```xml
<build>
        <plugins>
            <plugin>
                <groupId>com.easycode</groupId>
                <artifactId>maven-plugin-sql-codegen</artifactId>
                <version>1.0-SNAPSHOT</version>
                <configuration>
                    <config>
                        <basePackage>com.xxx.lighthouse</basePackage>
                    </config>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>SqlToEntityConvertor</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
```

### 生成效果

#### TableSchema.sql 文件部分示例

```mysql
DROP TABLE if exists `accounts`;
CREATE TABLE `accounts`
(
    `id`           bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`     varchar(16)         NOT NULL COMMENT '用户名',
    `password`     varchar(16)         NOT NULL COMMENT '密码',
    `account_type` tinyint(1)          NOT NULL DEFAULT '2' COMMENT '账号类型（1-内部员工，2-客户）',
    `phone`        varchar(20)         NOT NULL COMMENT '手机号',
    `email`        varchar(16)         NOT NULL COMMENT '邮箱',
    `created_at`   datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`   datetime            NOT NULL DEFAULT '1111-11-11 11:11:11' COMMENT '删除时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `username` (`username`, `deleted_at`) COMMENT '用户名需要唯一'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='客户公司-用户表';
```

#### Entity 生成示例

```java
package com.xxx.lighthouse.entities;

import lombok.Data;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableLogic;

/**
 * AccountsAutogenDO class
 * @Author maven-plugin-sql-codegen
 * @Description 客户公司-用户表
 **/
@Data
@TableName(value = "accounts")
public class AccountsAutogenDO {

    /**
     * 主键  DbType: bigint(20) UNSIGNED
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户名  DbType: varchar(16)
     */
    @TableField(value = "username")
    private String username;

    /**
     * 密码  DbType: varchar(16)
     */
    @TableField(value = "password")
    private String password;

    /**
     * 账号类型（1-内部员工，2-客户）  DbType: tinyint(1)
     */
    @TableField(value = "account_type")
    private Integer accountType;

    /**
     * 手机号  DbType: varchar(20)
     */
    @TableField(value = "phone")
    private String phone;

    /**
     * 邮箱  DbType: varchar(16)
     */
    @TableField(value = "email")
    private String email;

    /**
     * 创建时间  DbType: datetime
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间  DbType: datetime
     */
    @TableField(value = "updated_at", fill = FieldFill.UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 删除时间  DbType: datetime
     */
    @TableField(value = "deleted_at")
    @TableLogic(value = "1000-01-01 00:00:00", delval = "now()")
    private LocalDateTime deletedAt;

}
```

## maven-plugin-api-codegen

### 功能

通过 swagger 接口文档定义项目接口，解析swagger文档反向生成代码，节省开发时间，让研发人员把精力留在接口设计和代码实现上。

目前生成代码包括以下几类：

- Controller --springmvc接口handler处理器，【Controller只处理接口定义 @RequestBody和@RequestBody @ResponseBody以及设置自定义拦截器注解等作用】
- IService -- Controller 的 下层实现接口，【IService 只用于定义 handlerClass 的接口声明，定义 参数类型和返回类型
- FeignClient --用于封装 当前服务的http请求，实现sdk的作用供依赖服务调用
- DTO --以上定义中出现的DTO的定义

### 优点

与SpringMvc相关的注解以及接口定义，研发人员不用再手动编码。目前很多已存项目，研发人员为了方便开发直接在Controller层封装业务逻辑，不能很好的解耦，实现三层架构。后续如果控制层设计框架的转换其实改动很大，如果按照上面几类对不能层的定义进行区分，会让代码可读性更高。

还有一点就是，现在项目里面多是通过 swagger注解 来生成 接口文档，极大地浪费时间且与代码耦合度极高。

该插件就是为了解决以上问题，减少研发人员的工作量，以及接口定义与代码解耦。

### 使用

Maven pom.xml 中导入如下依赖

```xml
<build>
        <plugins>
            <plugin>
                <groupId>com.easycode</groupId>
                <artifactId>maven-plugin-api-codegen</artifactId>
                <version>1.0-SNAPSHOT</version>
                <configuration>
                    <config>
                        <!--                    <srcJavaPath>-->
                        <!--                        /Users/xx/code/src/main/java-->
                        <!--                    </srcJavaPath>-->
                        <basePackage>com.example</basePackage>
                    </config>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>ApiCodegenMojo</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
```

#### configuration

> 最后代码生成的位置，由配置拼接而成
>
> controller包根路径 = $srcJavaPath+$basePackage+$controllerPackageName
>
> Service包根路径 = $srcJavaPath+$basePackage+$servicePackageName
>
> controller包根路径 = $srcJavaPath+$basePackage+$controllerPackageName

##### generateType

- 描述：定义了代码生成类型，目前仅支持 springmvc 和 feignclient 两种，值不区分大小写。
- 当为 springmvc 时，会生成 Controller IService DTO 类型的代码
- 当为 feignclient时，会生成 FeignClient DTO类型的代码

##### apiDefineType

- 描述：接口定义模式，定义了当前需要生成的接口的解析源。当前默认只有swagger
- 默认值为：swagger，可不填此配置

##### apiDefineDirPath

- 描述：接口定义文件的目录
- 默认值为：${当前maven项目resources下面的api目录}，可不填此配置

##### srcJavaPath

- 描述：java代码目录。
- 默认值：${当前maven项目 src/main/java}
- 如果是代码和定义不在同一maven项目，此配置可以配置绝对路径

##### basePackage

- 描述：handlerClass dto service feignclient这些包的上级包名，类似于springboot的SpringbootApplication所在包

##### dtoPackageName

- 描述：生成的dto所在包名
- 默认值：dtos

##### controllerPackageName

- 描述：生成的controller所在包名
- 默认值：controllers

##### servicePackageName

- 描述：生成的service所在包名
- 默认值：services

##### feignClientPackageName

- 描述：生成的FeignClient所在包名
- 默认值：feignclients

##### applicationName

- 描述：用于生成FeignClient Bean Name以及生成@FeignClient的url属性，generateType=feignclient时必填

##### applicationServerPort

- 描述：用于生成@FeignClient的url属性，generateType=feignclient时必填

#### Swagge入门

##### 官方文档

https://swagger.io/specification/

##### 在线测试

https://editor.swagger.io/

可以在这个网站上，编写 swagger 文档，看一下展示效果，所见即所得（在mock api也是此效果）

如果打不开，可在本地docker上运行本地镜像服务

```bash
docker pull swaggerapi/swagger-editor
docker run -d -p 80:8080 swaggerapi/swagger-editor
```

https://github.com/ymfe/yapi  yapi — swagger api管理平台

##### 示例讲解 —对应生成的文件看末尾

```yaml
# swagger 文档版本
swagger: "2.0"
# 当前文件信息
info:
  version: 1.0.0
  title: 测试文件

# 相当于Controller Class 类路径上面的 @RequestMapping("/web-mgmt")
basePath: /web-mgmt

# 一个tag就相当于一个类，下面的path可以绑定在不同的tag下。建议一个yaml里面就一个tag，所以path都绑定这个tag
# 多tag的场景一般是 多个Controller需要依赖相同的definition定义。比如给b端的接口定义，给c端的接口定义，分为两个controller
tags:
  - name: TestApi # Controller class name
    description: 测试使用 # Controller class description

# 下面的协议暂时无用，默认写http就好，目前接口全是http支持。后续可能支持ws协议
schemes:
  - http

# 相当于@RequestMapping(consumes={"application/json"})
consumes:
  - application/json

# 相当于@RequestMapping(produces={"application/json"})
produces:
  - application/json

# 定义公共参数，可以在 path->operation->parameters 引用
# example:
#
# parameters:
#  - $ref: '#/parameters/PageNum'
parameters:
  PageNum:
    name: pageNum
    in: query
    description: 页数
    required: true
    type: integer
    format: int32
    default: 1
  PageSize:
    name: pageSize
    in: query
    description: 页面记录数
    required: true
    type: integer
    format: int32
    default: 25
paths:
  # path,相当于 handler method 上面 的 @RequestMapping(url="/v1/accounts")
  /v1/accounts:
    #Custom-Extension -- 当前 path 的的公共注解
    # 形如：
    # x-@注解名:
    #   属性名: 属性值
    # 相当于 @CommonAnnotation(x=33,y="str")
    x-@CommonAnnotation:
      x: 33
      y: "str"

    # path + operation = handler method , 到了这里真正开始定义 spring mvc 的handler方法
    # 相当于 @RequestMapping(url="/v1/accounts",method = RequestMethod.GET)
    get:
      #Custom-Extension -- 当前方法的注解
      # 相当于 @GetAnnotation(x=666,y="str666")
      x-@GetAnnotation:
        x: 666
        y: "str666"

      # 指明 当前 handler 属于哪个 handlerClass，一个 tag 相当于一个 handlerClass （仅绑定一个就好）
      tags:
        - TestApi

      # 下面两个属性用于生成 java method doc
      # 示例如下：
      # /**
      # * @summary 简单概括功能
      # * @description 详细描述
      # */
      summary: 查询账号列表
      description: 根据条件查询账号列表

      # handler method name
      operationId: listAccounts

      # 当前方法参数
      parameters:
        - name: name
          in: query
          description: 用户名称（模糊查找）
          type: string
        - name: phone
          in: query
          description: 用户手机号（模糊查找）
          type: string
        - name: email
          in: query
          description: 用户邮箱（模糊查找）
          type: string
        - $ref: '#/parameters/PageNum'
        - $ref: '#/parameters/PageSize'
      # 方法返回值
      responses:
        200:
          description: 账号分页查询数据
          schema:
            # 返回类型为定义的 ListAccountsResult 对象
            $ref: '#/definitions/ListAccountsResult'
    post:
      tags:
        - TestApi
      summary: 添加账号
      description: 新建账号
      operationId: createAccount
      parameters:
        - name: data
          in: body
          description: 账号信息
          schema:
            $ref: '#/definitions/AccountDetails'
          # 是否必填
          # 相当于 @NotNull
          required: true
      responses:
        200:
          # 方法返回值描述
          description: Ok
          # 返回值为 String
          schema:
            type: string
  /v1/accounts/{accountId}:
    parameters:
      # 路径参数，name一定要和路径上对应
      - name: accountId
        in: path
        type: integer
        format: int64
        required: true
        description: 账号id
    get:
      tags:
        - TestApi
      summary: 查询单个账号信息
      description: 根据id查询账号
      operationId: getAccount
      responses:
        200:
          description: 账号信息
          schema:
            $ref: '#/definitions/AccountDetails'
    delete:
      tags:
        - TestApi
      summary: 删除单个账号
      description: 根据id删除账号
      operationId: deleteAccount
      responses:
        200:
          description: ok
          schema:
            # 返回值类型为 CustomObject
            type: object
            x-format: CustomObject
            # 相当于 import com.java.CustomObject;
            x-import: com.java.CustomObject

    put:
      tags:
        - TestApi
      summary: 全量更新账号信息
      description: 根据id全量更新账号
      operationId: updateAccount
      parameters:
        - name: data
          in: body
          description: 更新数据
          required: true
          schema:
            $ref: '#/definitions/AccountDetails'
      responses:
        200:
          description: ok
          schema:
            type: object
            x-format: Map<String,String>
            x-import: java.util.Map
# 定义 dto
definitions:
  ListAccountsResult:
    description: 账号分页数据
    type: object
    properties:
      map:
        description: 哈哈
        type: object
        x-format: Result<TestBean>
        # 多个外部类导入
        x-import: com.xxx.Result;com.xxx.TestBean
      datas:
        type: array
        items:
          $ref: '#/definitions/AccountDetails'
      total:
        description: 总数
        type: integer
        format: int64
  AccountDetails:
    description: 账号信息
    type: object
    properties:
      idInt:
        description: 账号id
        type: integer
        format: int32
      idLong:
        description: 账号id
        type: integer
        format: int64
      id:
        description: 账号id
        type: integer
        format: int64
      username:
        description: 用户名
        type: string
      password:
        description: 密码
        type: string
      accountType:
        description: 账号类型（1-内部员工，2-客户）
        type: integer
        format: int32
      phone:
        description: 手机号
        type: string
      email:
        description: 邮箱
        type: string
      createdAt:
        description: 创建时间
        type: string
        format: date
        # 属性上的注解
        x-@Hello:
          xx: xx
          xy: xx
      updatedAt:
        description: 更新时间
        type: string
        format: date
    # 为这些属性加上 @NotNull
    required:
      - username
      - password
      - accountType
      - phone
      - email
```

##### basePath

相当于类上的 @RequestMapping,会作用于当前文件中所有定义的tag

##### tags

每一个tag定义，最后都会变成一个类

name对应类名

description用于生成类注释

##### schema

目前仅支持 http协议 即 springmvc 这一套接口开发模板代码生成；

##### parameters

公共参数定义，每个 operation中的parameters 可引用；避免不同operation中重复定义相同参数

> 目前parameter中in的取值分三种：query、body、path
>
> query：所有query参数最后会生成一个QueryParams DTO；一般是 GET DELETE 请求使用
>
> body：当in=body时，一般不定义type，使用schema引用一个定义的definition dto；一般用于POST
>
> path：当in=path时，说明此参数为路径参数，name字段需要和url里面的占位符名称对应;所有请求方法都可使用

##### type and format

在swagger中定义类型需要通过 type 和 format来指定。format相当于扩展

| type    | format    | java type               |
| ------- | --------- | ----------------------- |
| integer |           | java.lang.Integer       |
| integer | int32     | java.lang.Integer       |
| integer | int64     | java.lang.Long          |
| string  |           | java.lang.String        |
| string  | date      | java.time.LocalDate     |
| string  | date-time | java.time.LocalDateTime |
| number  |           | java.math.BigDecimal    |
| number  | float     | java.lang.Float         |
| number  | double    | java.lang.Double        |
| boolean |           | java.lang.Boolean       |
| object  |           | java.lang.Object        |

##### x-format

自定义、Map、泛型等类型的支持。当使用此属性时，type=object

##### x-import

一般配合 x-format 使用，当 x-format 引入了自定义类型或者需要手动import类时，x-import可用于导入类

如果有多个类需要导入，通过 ; 分开，不区分中英文

##### x-@注解名

用于给类上方法上属性上加上注解。如果注解需要导入类，也需要添加x-import导入类

##### 方法返回值为 void，如何定义？

```yaml
# 以下两种都可
responses:
        200:
          # 方法返回值描述
          description:  描述
          
responses:
        200: {}
```

##### 列表类型如何定义

```yaml
# List<String>
arr_1:
        type: array
        items:
          type: string
# List<ItemDefiniiton>    ItemDefiniiton需要在definition中定义         
arr_1:
        type: array
        items:
        	$ref: '#/definitions/ItemDefiniiton'
# List<Object>
arr_1:
        type: array
        items:
          type: object
```

##### 生成的文件

```java
package com.xxx.lighthouse.controllers;

import java.util.Map;
import com.java.CustomObject;
import com.xxx.lighthouse.dtos.*;
import com.xxx.lighthouse.services.ITestApiAutogenService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.stereotype.Controller;

/**
 * @class-name TestApiAutogenController
 * @description 测试使用
 * @author api-codegen
 **/
@Controller
@RequestMapping("/web-mgmt")
@Validated
public class TestApiAutogenController {

    private final ITestApiAutogenService service;

    public TestApiAutogenController(ITestApiAutogenService service) {
        this.service = service;
    }

    /**
     * <pre>
     * 查询账号列表 @Summary
     * </pre>
     * 根据条件查询账号列表 @Description
     *
     * @param queryParams query参数,详情参考dto定义
     * @return 账号分页查询数据
     */
    @CommonAnnotation(x = 33, y = "str")
    @GetAnnotation(x = 666, y = "str666")
    @ResponseBody
    @RequestMapping(value = "/v1/accounts",
            method = RequestMethod.GET,
            produces = {"application/json"},
            consumes = {"application/json"})
    public ListAccountsResultAutogenDTO listAccounts(@Validated ListAccountsQueryParamAutogenDTO queryParams) {
        return service.listAccounts(queryParams);
    }

    /**
     * <pre>
     * 添加账号 @Summary
     * </pre>
     * 新建账号 @Description
     *
     * @param data 账号信息
     * @return Ok
     */
    @CommonAnnotation(x = 33, y = "str")
    @ResponseBody
    @RequestMapping(value = "/v1/accounts",
            method = RequestMethod.POST,
            produces = {"application/json"},
            consumes = {"application/json"})
    public String createAccount(@Validated @RequestBody AccountDetailsAutogenDTO data) {
        return service.createAccount(data);
    }

    /**
     * <pre>
     * 查询单个账号信息 @Summary
     * </pre>
     * 根据id查询账号 @Description
     *
     * @param accountId 账号id
     * @return 账号信息
     */
    @ResponseBody
    @RequestMapping(value = "/v1/accounts/{accountId}",
            method = RequestMethod.GET,
            produces = {"application/json"},
            consumes = {"application/json"})
    public AccountDetailsAutogenDTO getAccount(@PathVariable("accountId") Long accountId) {
        return service.getAccount(accountId);
    }

    /**
     * <pre>
     * 全量更新账号信息 @Summary
     * </pre>
     * 根据id全量更新账号 @Description
     *
     * @param accountId 账号id
     * @param data 更新数据
     * @return ok
     */
    @ResponseBody
    @RequestMapping(value = "/v1/accounts/{accountId}",
            method = RequestMethod.PUT,
            produces = {"application/json"},
            consumes = {"application/json"})
    public Map<String,String> updateAccount(@PathVariable("accountId") Long accountId,
            @Validated @RequestBody AccountDetailsAutogenDTO data) {
        return service.updateAccount(accountId,
                data);
    }

    /**
     * <pre>
     * 删除单个账号 @Summary
     * </pre>
     * 根据id删除账号 @Description
     *
     * @param accountId 账号id
     * @return ok
     */
    @ResponseBody
    @RequestMapping(value = "/v1/accounts/{accountId}",
            method = RequestMethod.DELETE,
            produces = {"application/json"},
            consumes = {"application/json"})
    public CustomObject deleteAccount(@PathVariable("accountId") Long accountId) {
        return service.deleteAccount(accountId);
    }


}
```

```java
package com.xxx.lighthouse.dtos;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * @class-name AccountDetailsAutogenDTO
 * @description 账号信息
 * @author api-codegen
 **/
@Data
@JsonInclude(Include.NON_NULL)
public class AccountDetailsAutogenDTO {


    /**
     * 账号id
     */
    private Integer idInt;

    /**
     * 账号id
     */
    private Long idLong;

    /**
     * 账号id
     */
    private Long id;

    /**
     * 用户名
     */
    @NotNull
    private String username;

    /**
     * 密码
     */
    @NotNull
    private String password;

    /**
     * 账号类型（1-内部员工，2-客户）
     */
    @NotNull
    private Integer accountType;

    /**
     * 手机号
     */
    @NotNull
    private String phone;

    /**
     * 邮箱
     */
    @NotNull
    private String email;

    /**
     * 创建时间
     */
    @Hello(xx = "xx", xy = "xx")
    private LocalDate createdAt;

    /**
     * 更新时间
     */
    private LocalDate updatedAt;

}
```

```java
package com.xxx.lighthouse.dtos;

import javax.validation.constraints.NotNull;

/**
 * @class-name ListAccountsQueryParamAutogenDTO
 * @description listAccounts方法查询参数
 * @author api-codegen
 **/
public class ListAccountsQueryParamAutogenDTO {


    /**
     * 用户名称（模糊查找）
     */
    private String name;

    /**
     * 用户手机号（模糊查找）
     */
    private String phone;

    /**
     * 用户邮箱（模糊查找）
     */
    private String email;

    /**
     * 页数
     */
    @NotNull
    private Integer pageNum = 1;

    /**
     * 页面记录数
     */
    @NotNull
    private Integer pageSize = 25;

}
```

```java
package com.xxx.lighthouse.dtos;

import com.xxx.Result;
import com.xxx.TestBean;
import java.util.List;
import javax.validation.Valid;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * @class-name ListAccountsResultAutogenDTO
 * @description 账号分页数据
 * @author api-codegen
 **/
@Data
@JsonInclude(Include.NON_NULL)
public class ListAccountsResultAutogenDTO {


    /**
     * 哈哈
     */
    private Result<TestBean> map;

    /**
     * ${field.description}
     */
    @Valid
    private List<AccountDetailsAutogenDTO> datas;

    /**
     * 总数
     */
    private Long total;

}
```

```java
package com.xxx.lighthouse.services;

import java.util.Map;
import com.java.CustomObject;
import com.xxx.lighthouse.dtos.*;

/**
 * @interface-name ITestApiAutogenService
 * @description 测试使用
 * @author api-codegen
 **/
public interface ITestApiAutogenService {


    /**
     * <pre>
     * 查询账号列表 @Summary
     * </pre>
     * 根据条件查询账号列表 @Description
     *
     * @param queryParams query参数,详情参考dto定义
     * @return 账号分页查询数据
     */
    public ListAccountsResultAutogenDTO listAccounts(ListAccountsQueryParamAutogenDTO queryParams);

    /**
     * <pre>
     * 添加账号 @Summary
     * </pre>
     * 新建账号 @Description
     *
     * @param data 账号信息
     * @return Ok
     */
    public String createAccount(AccountDetailsAutogenDTO data);

    /**
     * <pre>
     * 查询单个账号信息 @Summary
     * </pre>
     * 根据id查询账号 @Description
     *
     * @param accountId 账号id
     * @return 账号信息
     */
    public AccountDetailsAutogenDTO getAccount(Long accountId);

    /**
     * <pre>
     * 全量更新账号信息 @Summary
     * </pre>
     * 根据id全量更新账号 @Description
     *
     * @param accountId 账号id
     * @param data 更新数据
     * @return ok
     */
    public Map<String,String> updateAccount(Long accountId,
            AccountDetailsAutogenDTO data);

    /**
     * <pre>
     * 删除单个账号 @Summary
     * </pre>
     * 根据id删除账号 @Description
     *
     * @param accountId 账号id
     * @return ok
     */
    public CustomObject deleteAccount(Long accountId);


}
```