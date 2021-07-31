# 分布式事务

[TOC]

*特别说明：本Markdown中的中间件的实际IP地址，需要根据实际的IP地址进行调整。而且Centos系统都配置有防火墙，如果在不禁用防火墙的前提下，开发者要根据实际需要依次打开相对应的端口号；如果禁用防火墙的话，则可以忽略。*



## 1、Nacos配置

### 1.1、Nacos准备

* 1、下载Nacos压缩包并解压

* 2、执行Nacos脚本

    * 在Nacos/conf目录下有一个`nacos-mysql.sql`脚本文件，并在本地数据库或者环境上执行；

* 3、修改Nacos配置文件
	
	* 在Nacos/conf目录下有一个`application.properties`文件，同时还有一个`application.properties.example`的参考文件，可以参考里面的配置内容来配置，这里直接把修改的内容粘贴出来了：
	
	  ```properties
	  spring.datasource.platform=mysql
	  
	  db.num=1
	  
	  db.url.0=jdbc:mysql://ip:3306/nacos?characterEncoding=utf8&connectTimeout=1000&socketTimeout=3000&autoReconnect=true&useUnicode=true&useSSL=false&serverTimezone=Asia/Shanghai
	  db.user='username'
	  db.password='password'
	  ```
	
	  


### 1.2、Nacos的启动

这里为了方便，则只启动单机Nacos。

如果是Windows系统的话，执行如下命令：

```text
# 启动nacos
startup.cmd -m standalone

# 关闭nacos
shutdown.cmd
```
如果使用Linux系统的话，执行如下命令：
```text
# 启动nacos
sh startup.sh -m standalone

# 关闭nacos
sh shutdown.sh
```

Nacos启动之后，打开浏览器，输入`http://ip:8848/nacos`， 用户名密码默认`nacos/nacos`，就能看到Nacos界面了。

如果使用Linux的话，需要把8848端口打开或者直接把防火墙firewalld关闭禁用。



### 1.3、Nacos项目配置

根据每个项目，配置不同的内容，此处省略。但是如果使用`spring-cloud-starter-alibaba-nacos-xxx`的话，Spring和Nacos集成的配置项必须要用`bootstrap.yml`或者`bootstrap.properties`才行。



## 2、分布式事务的前奏

### 2.1、分布式理论基础

- **事务**：事务是由一组操作构成的可靠的独立的工作单元，事务具备ACID的特性，即原子性、一致性、隔离性和持久性。

- **本地事务**：当事务由资源管理器本地管理时被称作本地事务。本地事务的优点就是支持严格的**ACID**特性，高效，可靠，状态可以只在资源管理器中维护，而且应用编程模型简单。但是本地事务不具备分布式事务的处理能力，隔离的最小单位受限于资源管理器。

- **全局事务**：当事务由全局事务管理器进行全局管理时成为全局事务，事务管理器负责管理全局的事务状态和参与的资源，协同资源的一致提交回滚。

- **TX协议**：应用或者应用服务器与事务管理器的接口。

- **XA协议**：全局事务管理器与资源管理器的接口。XA是由X/Open组织提出的分布式事务规范。该规范主要定义了全局事务管理器和局部资源管理器之间的接口。主流的数据库产品都实现了XA接口。XA接口是一个双向的系统接口，在事务管理器以及多个资源管理器之间作为通信桥梁。之所以需要XA是因为在分布式系统中从理论上讲两台机器是无法达到一致性状态的，因此引入一个单点进行协调。由全局事务管理器管理和协调的事务可以跨越多个资源和进程。全局事务管理器一般使用XA二阶段协议与数据库进行交互。

- **AP**：应用程序，可以理解为使用DTP（Data Tools Platform）的程序。

- **RM**：资源管理器，这里可以是一个DBMS或者消息服务器管理系统，应用程序通过资源管理器对资源进行控制，资源必须实现XA定义的接口。资源管理器负责控制和管理实际的资源。

- **TM**：事务管理器，负责协调和管理事务，提供给AP编程接口以及管理资源管理器。事务管理器控制着全局事务，管理事务的生命周期，并且协调资源。

- **两阶段提交协议**：XA用于在全局事务中协调多个资源的机制。TM和RM之间采取两阶段提交的方案来解决一致性问题。两节点提交需要一个协调者（**TM**）来掌控所有参与者（**RM**）节点的操作结果并且指引这些节点是否需要最终提交。两阶段提交的局限在于协议成本，准备阶段的持久成本，全局事务状态的持久成本，潜在故障点多带来的脆弱性，准备后，提交前的故障引发一系列隔离与恢复难题。

- **CAP定理**：对于共享数据系统，最多只能同时拥有**CAP**其中的两个，任意两个都有其适应的场景，真是的业务系统中通常是ACID与CAP的混合体。分布式系统中最重要的是满足业务需求，而不是追求高度抽象，绝对的系统特性。

  - C（**Consistency**）表示一致性，也就是所有用户看到的数据是一样的。
  - A（**Availability**）表示可用性，是指总能找到一个可用的数据副本。
  - P（**Partition tolerance**）表示分区容错性，能够容忍网络中断等故障。

- **BASE理论**：它对**CAP**中一致性和可用性权衡的结果，其来源于对大规模互联网分布式系统实践的总结，是基于CAP定律逐步演化而来。其核心思想是即使无法做到强一致性，但每个应用都可以根据自身业务特点，才用适当的方式来使系统打到最终一致性。

  - BA（**Basically Available**）指的是基本业务可用性，支持分区失败；
  - S（**Soft State**）表示柔性状态，也就是允许短时间内不同步；
  - E（**Eventually Consistent**）表示最终一致性，数据最终是一致的，但是实时是不一致的。
  - 原子性和持久性必须从根本上保障，为了可用性、性能和服务降级的需要，只有降低一致性和隔离性的要求。

- 柔性事务中的服务模式：

  1. 可查询操作：服务操作具有全局唯一的标识，操作唯一的确定的时间。
  2. 幂等操作：重复调用多次产生的业务结果与调用一次产生的结果相同。一是通过业务操作实现幂等性，二是系统缓存所有请求与处理的结果，最后是检测到重复请求之后，自动返回之前的处理结果。
  3. TCC操作：Try阶段，尝试执行业务，完成所有业务的检查，实现一致性；预留必须的业务资源，实现准隔离性。Confirm阶段：真正的去执行业务，不做任何检查，仅适用Try阶段预留的业务资源，Confirm操作还要满足幂等性。Cancel阶段：取消执行业务，释放Try阶段预留的业务资源，Cancel操作要满足幂等性。TCC与2PC(两阶段提交)协议的区别：TCC位于业务服务层而不是资源层，TCC没有单独准备阶段，Try操作兼备资源操作与准备的能力，TCC中Try操作可以灵活的选择业务资源，锁定粒度。TCC的开发成本比2PC高。实际上TCC也属于两阶段操作，但是TCC不等同于2PC操作。
  4. 可补偿操作：Do阶段：真正的执行业务处理，业务处理结果外部可见。Compensate阶段：抵消或者部分撤销正向业务操作的业务结果，补偿操作满足幂等性。约束：补偿操作在业务上可行，由于业务执行结果未隔离或者补偿不完整带来的风险与成本可控。实际上，TCC的Confirm和Cancel操作可以看做是补偿操作。

  

### 2.2、分布式事务产生的原因

#### 2.2.1、数据库的分库分表

讲到事务，又得搬出经典的银行转账问题了，下面以实例说明：

> 假设银行(bank)中有两个客户(name)张三和李四， 我们需要将张三的1000元存款(sal)转到李四的账户上，目标就是张三账户减1000，李四账户加1000，不能出现中间其它步骤(张三减1000，李四没加)。

![转账案例](./images/转账案例.png)

如果两个用户对应的银行存款数据在一个数据源中，即一个数据库中，通过Spring框架下的`@Transactional`注解来保证单一数据源增删改查的一致性，但是随着业务的不断扩大，用户数在不断变多，几百万几千万用户时数据可以存一个库甚至一个表里，假设有10个亿的用户？

为了解决数据库上的瓶颈，分库是很常见的解决方案，不同用户就可能落在不同的数据库里，原来一个库里的事务操作，现在变成了跨数据库的事务操作。

![转账案例2](./images/转账案例2.png)

此时`@Transactional`注解就失效了，这就是**跨数据库分布式事务问题**。

当我们的单个数据库的性能产生瓶颈的时候，我们可能会对数据库进行分区，这里所说的分区指的是物理分区，分区之后可能不同的库就处于不同的服务器上了，这个时候单个数据库的ACID已经不能适应这种情况了，而在这种ACID的集群环境下，再想保证集群的ACID几乎是很难达到，或者即使能达到那么效率和性能会大幅下降，最为关键的是再很难扩展新的分区了，这个时候如果再追求集群的ACID会导致我们的系统变得很差。



#### 2.2.2、微服务化的出现

随着业务不断增长，将业务中不同模块服务拆分成微服务后，同时调用多个微服务，同样会产生的分布式事务问题。

微服务化的银行转账情景往往是这样的

1. 调用交易系统服务创建交易订单；
2. 调用支付系统记录支付明细；
3. 调用账务系统执行 A 扣钱；
4. 调用账务系统执行 B 加钱;

![微服务化的转账案例](./images/微服务化的转账案例.png)

如图所示，每个系统都对应一个独立的数据源，且可能位于不同机房，同时调用多个系统的服务很难保证同时成功，这就是**跨服务分布式事务问题**。



## 3、分布式事务的解决方案

这时我们就需要引入一个新的理论原则来适应这种集群的情况，就是CAP原则或者叫CAP定理。但是在如今的分布式系统中，在任何数据库设计中，一个Web应用至多只能同时支持上面的两个属性。显然，任何横向扩展策略都要依赖于数据分区。因此，设计人员必须在一致性与可用性之间做出选择。

**这个定理在迄今为止的分布式系统中都是适用的！**

解决方案大体可以分为以下几种形式，各形式适用的场景各不相同，意思就是说，**即使当前场景适用，换到其它场景可能就不一定适用**。

* 2PC：两阶段提交协议

* 3PC：三阶段提交

* TCC事务补偿

* 本地消息表

* 基于可靠消息的最终一致性方案概述（消息事务）

* 最大努力通知



## 4、分布式事务解决方案之2PC（两阶段提交协议）

### 4.1、基础理论及流程

两阶段协议可以用于单机集中式系统，由事务管理器协调多个资源管理器；也可以用于分布式系统，**「由一个全局的事务管理器协调各个子系统的局部事务管理器完成两阶段提交」**。

这个协议有**「两个角色」**，

* **事务的协调者**；
* **事务的参与者**。

事务的提交又分成两个阶段：

* **投票阶段**：协调者将通知事务参与者准备提交或取消事务，然后进入表决过程。参与者将告知协调者自己的决策：同意（事务参与者本地事务执行成功，但未提交）或取消（本地事务执行故障）
* **决定阶段**：收到参与者的通知后，协调者再向参与者发出通知，根据反馈情况决定各参与者是否要提交还是回滚；

![2PC全流程](./images/2PC全流程.png)

#### 4.1.1、投票阶段/准备阶段

第一个阶段是**「投票阶段」**。

- 1、协调者首先将命令**「写入日志」**；
- 2、**「发一个prepare命令」**给 B 和 C 节点这两个参与者；
- 3、B和C收到消息后，根据自己的实际情况，**「判断自己的实际情况是否可以提交」**；
- 4、将处理结果**「记录到日志」**系统；
- 5、将结果**「返回」**给协调者；

![2PC提交流程](./images/2PC提交流程.png)



#### 4.1.2、决定阶段/提交阶段

第二个阶段是**「决定阶段」**。

当 A 节点收到 B 和 C 参与者所有的确认消息后：

- 1、**「判断」**所有协调者**「是否都可以提交」**；
  - 如果可以则**「写入日志」**并且发起commit命令
  - 有一个不可以则**「写入日志」**并且发起abort命令
- 2、参与者收到协调者发起的命令，**「执行命令」**；
- 3、将执行命令及结果**「写入日志」**；
- 4、**「返回结果」**给协调者；

![2PC投票阶段](./images/2PC投票阶段.png)



### 4.2、可能会存在哪些问题？

- **「单点故障」**：一旦事务管理器出现故障，整个系统不可用
- **「数据不一致」**：在阶段二，如果事务管理器只发送了部分 commit 消息，此时网络发生异常，那么只有部分参与者接收到 commit 消息，也就是说只有部分参与者提交了事务，使得系统数据不一致。
- **「响应时间较长」**：整个消息链路是串行的，要等待响应结果，不适合高并发的场景
- **「不确定性」**：当事务管理器发送 commit 之后，并且此时只有一个参与者收到了 commit，那么当该参与者与事务管理器同时宕机之后，重新选举的事务管理器无法确定该条消息是否提交成功。



### 4.3、实践：使用Seata实现2PC

#### 4.3.1、下载Seata-Server文件

附上Seata项目的[Github链接](https://github.com/seata)，以及[官网文档链接](https://seata.io/zh-cn/index.html)，其中seata的脚本和配置文件都在其他版本的[script目录下](https://github.com/seata/seata/tree/develop/script)。

#### 4.3.2、修改Seata的配置文件registry.conf

修改registry.conf之前请先备份好原始文件，以免修改错误无法回滚。

```json
registry {
  # file 、nacos 、eureka、redis、zk、consul、etcd3、sofa
  type = "nacos"

  nacos {
    application = "seata-server"
    serverAddr = "ip:8848"
    group = "SEATA_GROUP"
    namespace = ""
    cluster = "default"
    username = "nacos"
    password = "nacos"
  }
}

config {
  # file、nacos 、apollo、zk、consul、etcd3
  type = "nacos"

  nacos {
    serverAddr = "ip:8848"
    namespace = ""
    group = "SEATA_GROUP"
    username = "nacos"
    password = "nacos"
    dataId = "seataServer.properties"
  }
}
```

#### 4.3.3、推送Seata配置到Nacos

* 拉取配置文件：

```text
https://github.com/seata/seata/blob/develop/script/config-center/config.txt
```

* 拉取推送脚本：

```text
https://github.com/seata/seata/blob/develop/script/config-center/nacos/nacos-config.sh
```

* 修改config.txt

```properties
#my_test_tx_group需要与客户端保持一致  default需要和服务端registry.conf中registry中的cluster保持一致
service.vgroupMapping.my_test_tx_group=default
store.mode=db
store.db.datasource=druid
store.db.dbType=mysql
store.db.driverClassName=com.mysql.cj.jdbc.Driver
store.db.url=jdbc:mysql://ip:3306/seata?useSSL=false&useUnicode=true&characterEncoding=UTF-8&allowMultiQueries=true&serverTimezone=Asia/Shanghai
store.db.user='username'
store.db.password='password'
store.db.minConn=5
store.db.maxConn=30
store.db.globalTable=global_table
store.db.branchTable=branch_table
store.db.queryLimit=100
store.db.lockTable=lock_table
store.db.maxWait=5000
```

* 将config.txt保存到Seata解压的根目录，并推送到Nacos中（Linux直接使用Shell推送，Windows可以借助Gitbash进行推送）

```text
sh nacos-config.sh -h ip(使用具体的Nacos服务器ip)
```

* 打开Nacos地址，能看到Seata的配置文件已经推送到了Nacos服务器上，如图所示：

  ![推送seata配置到nacos之后](./images/推送seata配置到nacos之后.png)



#### 4.3.4、重启Seata-Server

注意config.txt文件目录，默认放在解压的根目录下，而不是在seata-server-xxx目录下。如果重启成功，可以在Nacos上看到seata-server服务列表：

![推送seata配置到nacos之后seata-server服务列表](./images/推送seata配置到nacos之后seata-server服务列表.png)

#### 4.3.5、执行sql脚本

该两个文件已存放在项目根目录的sql/seata的目录中。

* 1、创建一个seata库，执行如下脚本：

```sql
create database seata character set utf8mb4;

use seata;
-- -------------------------------- The script used when storeMode is 'db' --------------------------------
-- the table to store GlobalSession data
CREATE TABLE IF NOT EXISTS `global_table`
(
    `xid`                       VARCHAR(128) NOT NULL,
    `transaction_id`            BIGINT,
    `status`                    TINYINT      NOT NULL,
    `application_id`            VARCHAR(32),
    `transaction_service_group` VARCHAR(32),
    `transaction_name`          VARCHAR(128),
    `timeout`                   INT,
    `begin_time`                BIGINT,
    `application_data`          VARCHAR(2000),
    `gmt_create`                DATETIME,
    `gmt_modified`              DATETIME,
    PRIMARY KEY (`xid`),
    KEY `idx_gmt_modified_status` (`gmt_modified`, `status`),
    KEY `idx_transaction_id` (`transaction_id`)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8;

-- the table to store BranchSession data
CREATE TABLE IF NOT EXISTS `branch_table`
(
    `branch_id`         BIGINT       NOT NULL,
    `xid`               VARCHAR(128) NOT NULL,
    `transaction_id`    BIGINT,
    `resource_group_id` VARCHAR(32),
    `resource_id`       VARCHAR(256),
    `branch_type`       VARCHAR(8),
    `status`            TINYINT,
    `client_id`         VARCHAR(64),
    `application_data`  VARCHAR(2000),
    `gmt_create`        DATETIME(6),
    `gmt_modified`      DATETIME(6),
    PRIMARY KEY (`branch_id`),
    KEY `idx_xid` (`xid`)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8;

-- the table to store lock data
CREATE TABLE IF NOT EXISTS `lock_table`
(
    `row_key`        VARCHAR(128) NOT NULL,
    `xid`            VARCHAR(128),
    `transaction_id` BIGINT,
    `branch_id`      BIGINT       NOT NULL,
    `resource_id`    VARCHAR(256),
    `table_name`     VARCHAR(32),
    `pk`             VARCHAR(36),
    `gmt_create`     DATETIME,
    `gmt_modified`   DATETIME,
    PRIMARY KEY (`row_key`),
    KEY `idx_branch_id` (`branch_id`)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8;
```

* 2、新建bank1服务对应的数据库distributed-tx-seata-bank1，执行如下sql：

```sql
create database distributed-tx-seata-bank1 character set utf8mb4;

use distributed-tx-seata-bank1;

-- for AT mode you must to init this sql for you business database. the seata server not need it.
CREATE TABLE IF NOT EXISTS `undo_log`
(
    `branch_id`     BIGINT       NOT NULL COMMENT 'branch transaction id',
    `xid`           VARCHAR(128) NOT NULL COMMENT 'global transaction id',
    `context`       VARCHAR(128) NOT NULL COMMENT 'undo_log context,such as serialization',
    `rollback_info` LONGBLOB     NOT NULL COMMENT 'rollback info',
    `log_status`    INT(11)      NOT NULL COMMENT '0:normal status,1:defense status',
    `log_created`   DATETIME(6)  NOT NULL COMMENT 'create datetime',
    `log_modified`  DATETIME(6)  NOT NULL COMMENT 'modify datetime',
    UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
    ) ENGINE = InnoDB
    AUTO_INCREMENT = 1
    DEFAULT CHARSET = utf8 COMMENT ='AT transaction mode undo table';

DROP TABLE IF EXISTS `t_account`;
CREATE TABLE `t_account`  (
  `id` bigint(0) NOT NULL,
  `account` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL comment '账户名',
  `balance` decimal(5, 2) NOT NULL comment '账户余额',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic comment='账户表';

INSERT INTO `t_account`(`id`, `account`, `balance`) VALUES (1, 'zhangsan', 100.00);

```
  ![](./images/zhangsan余额.png)

* 3、新建bank2服务对应的数据库distributed-tx-seata-bank2，执行如下sql：

```sql
create database distributed-tx-seata-bank2 character set utf8mb4;

use distributed-tx-seata-bank2;

-- for AT mode you must to init this sql for you business database. the seata server not need it.
CREATE TABLE IF NOT EXISTS `undo_log`
(
    `branch_id`     BIGINT       NOT NULL COMMENT 'branch transaction id',
    `xid`           VARCHAR(128) NOT NULL COMMENT 'global transaction id',
    `context`       VARCHAR(128) NOT NULL COMMENT 'undo_log context,such as serialization',
    `rollback_info` LONGBLOB     NOT NULL COMMENT 'rollback info',
    `log_status`    INT(11)      NOT NULL COMMENT '0:normal status,1:defense status',
    `log_created`   DATETIME(6)  NOT NULL COMMENT 'create datetime',
    `log_modified`  DATETIME(6)  NOT NULL COMMENT 'modify datetime',
    UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
    ) ENGINE = InnoDB
    AUTO_INCREMENT = 1
    DEFAULT CHARSET = utf8 COMMENT ='AT transaction mode undo table';

DROP TABLE IF EXISTS `t_account`;
CREATE TABLE `t_account`  (
  `id` bigint(0) NOT NULL,
  `account` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL comment '账户名',
  `balance` decimal(5, 2) NOT NULL comment '账户余额',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic comment='账户表';

INSERT INTO `t_account`(`id`, `account`, `balance`) VALUES (2, 'lisi', 0.00);

```
  ![](./images/lisi余额.png)

#### 4.3.6、将registry.conf复制到项目的资源目录下

貌似这步可以忽略

![拷贝registry.conf文件至项目资源目录resources下](./images/拷贝registry.conf文件至项目资源目录resources下.png)



#### 4.3.7、添加seata依赖

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-seata</artifactId>
    <exclusions>
        <exclusion>
            <groupId>io.seata</groupId>
            <artifactId>seata-spring-boot-starter</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<dependency>
    <groupId>io.seata</groupId>
    <artifactId>seata-spring-boot-starter</artifactId>
    <version>1.3.0</version>
</dependency>
```



#### 4.3.8、修改项目配置文件

##### 4.3.8.1、bank1项目的配置文件

配置如下：

```yaml
server:
  port: 8081

# 实际虚拟机地址
server-ip: 192.168.213.130

nacos-server: ${server-ip}:8848

spring:
  application:
    name: distributed-tx-seata-bank1
  cloud:
    nacos:
      config:
        server-addr: ${nacos-server}
        username: ${nacos-username:nacos}
        password: ${nacos-password:nacos}
        namespace: ${nacos-namespace:public}
        file-extension: yml
        extension-configs:
          - dataId: common_datasource.yml
            refresh: true
        enabled: true # 启用nacos配置，默认为true
        max-retry: 5
        config-long-poll-timeout: 30000
      discovery:
        username: ${nacos-username:nacos}
        password: ${nacos-password:nacos}
        server-addr: ${nacos-server}
        namespace: ${nacos-namespace:public}
#        register-enabled: true # 是否注册，默认true（注册）
#        enabled: true # 启用服务发现功能， 默认为true

# 防止nacos狂刷
logging:
  level:
    com.alibaba.nacos.client: error

ribbon:
  ConnectTimeout: 3000
  ReadTimeout: 6000

# seata服务器地址，默认为localhost:8091
seata:
  enabled: true
  service:
    grouplist:
      default: ${server-ip}:8091
```

其余配置项都存放在Nacos上：

common_datasource.yml

```yaml
mysql:
    host: 192.168.213.130
    username: caychen
    password: 1qaz@WSX
```

distributed-tx-seata-bank1.yml

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://${mysql.host}:3306/distributed-tx-seata-bank1?useSSL=false&useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&allowMultiQueries=true&serverTimezone=Asia/Shanghai
    username: ${mysql.username}
    password: ${mysql.password}
    hikari:
      # 最小空闲连接数量
      minimum-idle: 5
      # 空闲连接存活最大时间，默认600000（10分钟）
      idle-timeout: 180000
      # 连接池最大连接数，默认是10
      maximum-pool-size: 10
      # 此属性控制从池返回的连接的默认自动提交行为,默认值：true
      auto-commit: true
      # 连接池名称
      pool-name: MyHikariCP
      # 此属性控制池中连接的最长生命周期，值0表示无限生命周期，默认1800000即30分钟
      max-lifetime: 1800000
      # 数据库连接超时时间,默认30秒，即30000
      connection-timeout: 30000
      connection-test-query: SELECT 1
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: GMT+8
  cloud:
    alibaba:
      seata:
        # 事务分组配置，重要！重要！重要！
        tx-service-group: my_test_tx_group

mybatis-plus:
  configuration:
    # 驼峰下划线转换
    map-underscore-to-camel-case: true
    auto-mapping-behavior: full
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  mapper-locations: classpath*:mapper/**/*Mapper.xml
  global-config:
    # 逻辑删除配置
    db-config:
      # 删除前
      logic-not-delete-value: 1
      # 删除后
      logic-delete-value: 0
  type-aliases-package: com.caychen.seata.bank.entity
```

重点是要在配置文件中加入seata的事务分组，如下：


```yaml
#添加事务组
spring:
  cloud:
	alibaba:
	  seata:
		# 事务分组配置
		tx-service-group: my_test_tx_group
```



##### 4.3.8.2、bank2项目的配置文件

同bank1类似， 除了如下：

```yaml
server:
  # 端口不一样
  port: 8082

spring:
  application:
    # 项目名不一样
    name: distributed-tx-seata-bank2
    
  datasource:
    # 数据库名不一样
    url: jdbc:mysql://${mysql.host}:3306/distributed-tx-seata-bank2?useSSL=false&useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&allowMultiQueries=true&serverTimezone=Asia/Shanghai
```





#### 4.3.9、测试场景

* distributed-tx-bank1逻辑正常，distributed-tx-bank2逻辑正常，流程正常结束；
  * 测试省略；
* distributed-tx-bank1逻辑正常，distributed-tx-bank2逻辑异常；
  * bank1捕获bank2抛出的异常，因为加了本地事务，自然会正常回滚，测试省略；
* distributed-tx-bank1逻辑异常，distributed-tx-bank2无所谓；
  * bank1都异常了，不会走bank2逻辑，正常回滚，测试省略；
* （==**重点**==）distributed-tx-bank1逻辑正常，distributed-tx-bank2逻辑正常，然后在distributed-tx-bank1中手动抛异常，流程异常结束；

贴下bank1的转账逻辑代码，如下：

```java
@Transactional
@GlobalTransactional(rollbackFor = Exception.class)
public Boolean transfer(TransferRequest transferRequest) throws Exception {
    String xid = RootContext.getXID();
    log.info("全局事务ID: [{}]", xid);

    //转账金额
    BigDecimal transferRequestMoney = transferRequest.getMoney();

    Account account = accountMapper.selectById(transferRequest.getFromId());

    if (account.getBalance().compareTo(transferRequestMoney) == -1) {
        throw new Exception("余额不足, 无法转账");
    }

    //计算余额
    BigDecimal newBalance = account.getBalance().subtract(transferRequestMoney);
    account.setBalance(newBalance);
    int affectCount = accountMapper.updateById(account);
    Boolean isOk = affectCount == 1 ? true : false;

    //远程调用
    isOk = isOk && bank2FeignClient.transferMoney(transferRequest);

    //人为制造异常
    if (transferRequestMoney.compareTo(new BigDecimal("4")) == 0) {
        throw new Exception("人为制造异常");
    }

    return isOk;
}
```

为了方便演示Seata的分布式事务，以DEBUG模式启动bank1和bank2两个服务，并在bank1的人为制造异常的代码行打上断点，如下：

```java
if (transferRequestMoney.compareTo(new BigDecimal("4")) == 0)
```

使用IDEA自带的`generated-requests.http`功能快速生成接口，并执行：

```json
POST http://localhost:8080/v1/bank1/transfer
Accept: */*
Content-Type: application/json

{
  "fromId": 1,
  "toId": 2,
  "money": "4"
}
```

代码停留在上述断点处，依次查看seata数据库和业务undo_log表数据：

* seata库表

  * global_table

    ![](./images/global_table.png)

  * branch_table

    ![](./images/branch_table.png)

  * lock_table

    ![](./images/lock_table.png)

* 业务undo_log表

  ![](./images/bank2库的undo_log表.png)

一旦bank1最后抛出异常之后，seata就会回滚事务，包括全局事务和分支事务，如图所示：

![](./images/事务回滚.png)

回滚完成之后，seata库中的表数据和业务undo_log表数据会被清空掉。

正常提交事务的如下图：

![](./images/正常提交.png)





## 5、分布式事务解决方案之3PC（三阶段提交）

三阶段提交又称3PC，相对于2PC来说增加了CanCommit阶段和超时机制。如果一段时间内没有收到协调者的commit请求，那么就会自动进行commit，解决了2PC单点故障的问题。

但是性能问题和不一致问题仍然没有根本解决。下面我们还是一起看下三阶段流程的是什么样的？

- 第一阶段：**「CanCommit阶段」**这个阶段所做的事很简单，就是协调者询问事务参与者，你是否有能力完成此次事务。
  - 如果都返回yes，则进入第二阶段
  - 有一个返回no或等待响应超时，则中断事务，并向所有参与者发送abort请求
- 第二阶段：**「PreCommit阶段」**此时协调者会向所有的参与者发送PreCommit请求，参与者收到后开始执行事务操作，并将Undo和Redo信息记录到事务日志中。参与者执行完事务操作后（此时属于未提交事务的状态），就会向协调者反馈“Ack”表示我已经准备好提交了，并等待协调者的下一步指令。
- 第三阶段：**「DoCommit阶段」**在阶段二中，如果所有的参与者节点都可以进行PreCommit提交，那么协调者就会从“预提交状态”转变为“提交状态”。然后向所有的参与者节点发送"doCommit"请求，参与者节点在收到提交请求后就会各自执行事务提交操作，并向协调者节点反馈“Ack”消息，协调者收到所有参与者的Ack消息后完成事务。相反，如果有一个参与者节点未完成PreCommit的反馈或者反馈超时，那么协调者都会向所有的参与者节点发送abort请求，从而中断事务。





## 6、分布式事务解决方案之TCC事务补偿

### 基础理论

TCC 将事务提交分为 Try - Confirm - Cancel 3个操作。其和两阶段提交有点类似，Try为第一阶段，Confirm - Cancel为第二阶段，是一种**应用层面侵入业务的两阶段提交**。

其核心思想是：**「针对每个操作，都要注册一个与其对应的确认和补偿（撤销）操作」**。它分为三个阶段：

**「Try,Confirm,Cancel」**

- Try阶段主要是对**「业务系统做检测及资源预留」**，接下来又有两个阶段：
  - Confirm 阶段主要是对**「业务系统做确认提交」**，Try阶段执行成功并开始执行 Confirm阶段时，默认 Confirm阶段是不会出错的。即：只要Try成功，Confirm一定成功。
  - Cancel 阶段主要是在业务执行错误，需要回滚的状态下执行的业务取消，**「预留资源释放和撤销」**。



比如下一个订单减一个库存：

![下订单减库存的TCC流程](./images/下订单减库存的TCC流程.png)

执行流程：

- Try阶段：订单系统将当前订单状态设置为支付中，库存系统校验当前剩余库存数量是否大于1，然后将可用库存数量设置为库存剩余数量减1，
  - 如果Try阶段**「执行成功」**，则执行**Confirm**阶段，将订单状态修改为支付成功，库存剩余数量修改为可用库存数量；
  - 如果Try阶段**「执行失败」**，则执行**Cancel**阶段，将订单状态修改为支付失败，可用库存数量修改为库存剩余数量；

TCC 事务机制相比于上面介绍的2PC，解决了其几个缺点：

- 1.**「解决了协调者单点」**，由主业务方发起并完成这个业务活动。业务活动管理器也变成多点，引入集群。
- 2.**「同步阻塞」**：引入超时，超时后进行补偿，并且不会锁定整个资源，将资源转换为业务逻辑形式，粒度变小。
- 3.**「数据一致性」**，有了补偿机制之后，由业务活动管理器控制一致性。

总之，TCC 就是通过代码人为实现了两阶段提交，不同的业务场景所写的代码都不一样，并且很大程度的**「增加」**了业务代码的**「复杂度」**，因此，这种模式并不能很好地被复用。



### TCC需要解决的问题

#### 1、**幂等控制**

使用TCC时要注意Try - Confirm - Cancel 3个操作的**幂等控制**，因为网络原因或者重试操作都有可能导致这几个操作的重复执行。

![2PC的幂等问题](./images/2PC的幂等问题.png)

新建一张去重表，使用某些字段作为唯一建，若插入报错返回也是可以的，不管怎么样，核心就是保证，操作幂等性。



#### 2、**空回滚**

如下图所示，事务协调器在调用TCC服务的一阶段Try操作时，可能会出现因为丢包而导致的网络超时，此时事务协调器会触发二阶段回滚，调用TCC服务的Cancel操作；

TCC服务在未收到Try请求的情况下收到Cancel请求，这种场景被称为空回滚；TCC服务在实现时应当允许空回滚的执行；

![2PC的空回滚问题](./images/2PC的空回滚问题.png)

核心思想就是 **回滚请求处理时，如果对应的具体业务数据为空，则返回成功**

当然这种问题也可以通过中间件层面来实现，如，在第一阶段try()执行完后，向一张事务表中插入一条数据(包含事务id，分支id)，cancle()执行时，判断如果没有事务记录则直接返回，但是现在还不支持。



#### 3、**防悬挂**

如下图所示，事务协调器在调用TCC服务的一阶段Try操作时，可能会出现因网络拥堵而导致的超时，此时事务协调器会触发二阶段回滚，调用TCC服务的Cancel操作；在此之后，拥堵在网络上的一阶段Try数据包被TCC服务收到，出现了二阶段Cancel请求比一阶段Try请求先执行的情况；

用户在实现TCC服务时，应当允许空回滚，但是要拒绝执行空回滚之后到来的一阶段Try请求；

![2PC的悬挂问题](./images/2PC的悬挂问题.png)

这里又怎么做呢？

可以在二阶段执行时插入一条事务控制记录，状态为已回滚，这样当一阶段执行时，先读取该记录，如果记录存在，就认为二阶段回滚操作已经执行，不再执行try方法。





## 7、分布式事务解决方案之本地消息表

![本地消息表](./images/本地消息表.png)

执行流程：

- 消息生产方，需要额外建一个消息表，并**「记录消息发送状态」**。消息表和业务数据要在一个事务里提交，也就是说他们要在一个数据库里面。然后消息会经过MQ发送到消息的消费方。
  - 如果消息发送失败，会进行重试发送。
- 消息消费方，需要**「处理」**这个**「消息」**，并完成自己的业务逻辑。
  - 如果是**「业务上面的失败」**，可以给生产方**「发送一个业务补偿消息」**，通知生产方进行回滚等操作。
  - 此时如果本地事务处理成功，表明已经处理成功了
  - 如果处理失败，那么就会重试执行。
- 生产方和消费方定时扫描本地消息表，把还没处理完成的消息或者失败的消息再发送一遍。





## 8、分布式事务解决方案之基于可靠消息的最终一致性方案概述（消息事务）

### 8.1 、基础理论

消息事务的原理是将两个事务**「通过消息中间件进行异步解耦」**，和上述的本地消息表有点类似，但是是通过消息中间件的机制去做的，其本质就是“将本地消息表封装到了消息中间件中”。

执行流程：

- 发送prepare消息到消息中间件
- 发送成功后，执行本地事务
  - 如果事务执行成功，则commit，消息中间件将消息下发至消费端
  - 如果事务执行失败，则回滚，消息中间件将这条prepare消息删除
- 消费端接收到消息进行消费，如果消费失败，则不断重试。

这种方案也是实现了**「最终一致性」**，对比本地消息表实现方案，不需要再建消息表，**「不再依赖本地数据库事务」**了，所以这种方案更适用于高并发的场景。目前市面上实现该方案的**「只有阿里的 RocketMQ」**，后来阿里将 **RocketMQ** 捐赠给 **Apache** 软件基金会，如今已成为 **Apache** 的顶级项目。



### 8.2、RocketMQ执行流程

**RocketMQ** 事务消息设计则主要为了解决 **Producer** 端的消息发送与本地事务执行的原子性问题， **RocketMQ** 的设计中 **Broker** 与 **Producer** 端的双向通信能力，使得 **Broker** 天生可以作为一个事务协调者存在；而 **RocketMQ** 本身提供的存储机制为事务消息提供了持久化能力；**RocketMQ** 的高可用机制以及可靠消息设计则为事务消息在系统发生异常时一人能够保证达成事务的最终一致性。

![](./images/RocketMQ分布式事务交互流程.jpg)

借助上图来描述整个流程，其中：A服务为Producer消息发送方；B服务为MQ订阅方；

流程如下：

1.  **Producer 发送事务消息**

   Producer （MQ 发送方）服务发送事务消息至 MQ Server，MQ Server 将消息状态标记为Prepared（预备状态，也称半消息 half message），此时这条消息在消费者（MQ 订阅方）是无法消费到的。

2.  **MQ Server 回应消息发送成功**

   MQ Server接收到 Producer 发给的消息，则回应发送成功，表示MQ已经接收到消息。

3.  **Producer 执行本地事务**

   Producer 端执行业务逻辑，通过本地数据库事务控制。

4.  **消息投递**

   （1）、若 Producer 本地事务执行成功，则自动向 MQ Server 发送 commit 消息，MQ Server 接收到 commit 消息后，将之前投递的半消息的状态标记为可消费，此时 MQ 订阅方则能正常消费消息；

   （2）、若 Producer 本地事务执行失败，则自动向 MQ Server 发送 rollback 消息，MQ Server 接收到 rollback 消息后，会将原来的半消息进行删除；

   （3）、MQ 订阅方消费消息，消费成功则向 MQ 回应ack，否则将重复接收消息。这里的ack默认自动回应，即程序执行正常则自动回应ack。

5.  **事务回查**

   如果在执行 Producer 端本地事务过程中，由于网络问题，导致服务宕机、重启或者超时等异常信息，MQ Server 将会不停的询问同组的其他 Producer 来获取事务执行状态，这个过程叫做 **事务回查**， MQ Server 会根据事务回查的结果来决定是否投递消息。

以上主干流程已由 **RocketMQ** 实现，对于用户侧来说，只需要分别实现本地事务执行以及本地事务回查方法。

```JAVA
public interface RocketMQLocalTransactionListener {
    //本地事务执行
    RocketMQLocalTransactionState executeLocalTransaction(final Message msg, final Object arg);

    // 本地事务回查
    RocketMQLocalTransactionState checkLocalTransaction(final Message msg);
}
```




### 8.3、RocketMQ简单使用

#### 8.3.1、下载并解压

从 [RocketMQ](http://rocketmq.apache.org/dowloading/releases/) 官网下载对应的压缩包并解压，如果使用的是Centos系统的话，请先上传再解压（废话）。

![](./images/RocketMQ压缩包.png)



#### 8.3.2、启动RocketMQ（以Centos为例）

==**注意：RocketMQ 与 JDK11兼容性不好，该章节请尽量使用 jdk8，避免不必要的麻烦。**==

在启动 RocketMQ 之前，需要修改 namesrv 和 broker 对应的 jvm 内存，默认8G，在学习使用阶段可以放小点，不然 RocketMQ 启动不了。

进入RocketMQ 的 bin 目录，会看到 runserver.sh 和 runbroker.sh 两个脚本，使用 vi/vim，将 jvm 内存改为512m，如图：

* 先修改 runserver.sh 文件：

```
vim runserver.sh

修改jvm内存为512m

!wq
保存退出
```

![](./images/runserver修改jvm.png)



* 再修改 runbroker.sh 文件：

```
vim runbroker.sh

修改jvm内存为512m

!wq
保存退出
```

![](./images/runbroker修改jvm.png)



* 启动namesrv

  ```tex
  nohup sh bin/mqnamesrv &
  
  # 查看namesrv日志
  tail -f ~/logs/rocketmqlogs/namesrv.log
  ```

  ![](./images/namesrv启动日志.png)

  看到这句话，说明 namesrv 启动成功。

  

* 启动broker

  ```text
  nohup sh bin/mqbroker -n 实际服务ip:9876 &
  
  # 查看broker日志
  tail -f ~/logs/rocketmqlogs/broker.log 
  ```

  ![](./images/broker启动日志.png)

  看到这句话，说明 broker 启动成功。



#### 8.3.3、发送及接收消息

我们可以使用 **RocketMQ** 提供的自带的Demo进行收发消息。

依次打开两个console窗口，并进入到 RocketMQ 的 根目录下，依次执行如下命令：

```text
# 临时设置环境变量
export NAMESRV_ADDR=ip:9876

# 执行发送消息
sh bin/tools.sh org.apache.rocketmq.example.quickstart.Producer
```

和

```text
# 临时设置环境变量
export NAMESRV_ADDR=ip:9876

# 消费消息
sh bin/tools.sh org.apache.rocketmq.example.quickstart.Consumer
```

在发送端的console会看到如图所示：

![](./images/mq发送消息日志.png)

而在消费端的console会看到如图所示：

![](./images/消费者消费日志.png)



#### 8.3.4、关闭RocketMQ

```text
# 关闭broker
sh bin/mqshutdown broker

# 关闭namesrv
sh bin/mqshutdown namesrv
```



#### 8.3.5、RocketMQ控制台（rocketmq-console）

RocketMQ-Console是RocketMQ项目的扩展插件，是一个图形化管理控制台，提供Broker集群状态查看，Topic管理，Producer、Consumer状态展示，消息查询等常用功能，这个功能在安装好RocketMQ后需要额外单独安装、运行。

##### 8.3.5.1、下载

进入[rocketmq-externals](https://github.com/apache/rocketmq-externals)项目的GitHub地址，如下图，可看到RocketMQ项目的诸多扩展项目，其中就包含我们需要下载的rocketmq-console。

![](./images/RocketMQ-Console.png)



克隆项目到本地

```text
git clone  https://github.com/apache/rocketmq-externals.git
```

进入rocketmq-console项目文件夹下，修改src/main/resources/application.properties。

```properties
server.address=0.0.0.0
server.port=8080

### SSL setting
#server.ssl.key-store=classpath:rmqcngkeystore.jks
#server.ssl.key-store-password=rocketmq
#server.ssl.keyStoreType=PKCS12
#server.ssl.keyAlias=rmqcngkey

#spring.application.index=true
spring.application.name=rocketmq-console
spring.http.encoding.charset=UTF-8
spring.http.encoding.enabled=true
spring.http.encoding.force=true
logging.level.root=INFO
logging.config=classpath:logback.xml

##################################################
# update
#if this value is empty,use env value rocketmq.config.namesrvAddr  NAMESRV_ADDR | now, you can set it in ops page.default localhost:9876
rocketmq.config.namesrvAddr=localhost:9876
##################################################

#if you use rocketmq version < 3.5.8, rocketmq.config.isVIPChannel should be false.default true
rocketmq.config.isVIPChannel=
#rocketmq-console's data path:dashboard/monitor
rocketmq.config.dataPath=/tmp/rocketmq-console/data
#set it false if you don't want use dashboard.default true
rocketmq.config.enableDashBoardCollect=true
#set the message track trace topic if you don't want use the default one
rocketmq.config.msgTrackTopicName=
rocketmq.config.ticketKey=ticket

#Must create userInfo file: ${rocketmq.config.dataPath}/users.properties if the login is required
rocketmq.config.loginRequired=false

#set the accessKey and secretKey if you used acl
#rocketmq.config.accessKey=
#rocketmq.config.secretKey=
```

将项目打包成jar包。

```text
mvn clean package -Dmaven.test.skip=true
```

上传至服务器上，并执行。

```text
#如果配置文件没有填写Name Server的话，可以在启动项目时指定namesrvAddr
$ java -jar rocketmq-console-ng-xxx.jar --rocketmq.config.namesrvAddr=ip:9876

#因为本文在打包时配置了namesrvAddr，故而执行如下命令
$ java -jar rocketmq-console-ng-xxx.jar
```

为了让console服务能够后台一直运行，则执行如下命令：

```text
nohup java -jar rocketmq-console-ng-xxx.jar > log.file  2>&1 &
```

等服务起来之后，访问http://ip:8080端口，即可看到RocketMQ的控制台。如图所示：

![](./images/RocketMQ控制台.png)

具体怎么使用，不是本章的重点，此处忽略，能看即可。



### 8.4、实践：使用RocketMQ实现消息最终一致性

#### 8.4.1、启动RocketMQ

如果使用的是Linux/Centos部署的RocketMQ的话，那么需要注意以下问题：==在启动之前，需要修改broker的映射地址，因为发送消息的时候，是往broker的topic通道中发送，所以broker的地址需要能在外网访问，默认启动broker的时候是以内网ip作为broker地址，这样外部程序就无法连接，导致发送失败，则需要修改成外部程序能够访问的ip。当然如果是放在同一套环境下或者内网ip能够访问的情况，可以忽略该步骤。==

```properties
# 在Rocket的conf目录下有个broker.conf文件，新增一行配置信息

###################################
#add， 最好是公网ip，避免虚拟机地址一直变更
brokerIP1=192.168.213.130
###################################
```

完成修改之后，然后分别执行启动namesrv和broker：

```text
# 启动namesrv
nohup sh bin/mqnamesrv &

# 启动broker，指定配置文件和namesrv，并让其自动创建topic
nohup bin/mqbroker -n 实际服务器ip:9876 -c /usr/local/rocketmq/rocketmq-all-4.8.0-bin-release/conf/broker.conf autoCreateTopicEnable=true &
```

使用控制台console就能看到broker的ip就变成外部程序能够访问的ip了。

![](./images/broker的外网地址.png)

#### 8.4.2、执行sql脚本

该两个文件已存放在项目根目录的sql/msg的目录中。

* 1、新建bank1服务对应的数据库distributed-tx-msg-bank1，执行如下sql：

```sql
create database distributed-tx-msg-bank1 character set utf8mb4;

use distributed-tx-msg-bank1;

DROP TABLE IF EXISTS `t_account`;
CREATE TABLE `t_account`  (
  `id` bigint(0) NOT NULL,
  `account` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL comment '账户名',
  `balance` decimal(5, 2) NOT NULL comment '账户余额',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic comment='账户表';

INSERT INTO `t_account`(`id`, `account`, `balance`) VALUES (1, 'zhangsan', 100.00);

DROP TABLE IF EXISTS `tx_duplication`;
CREATE TABLE `tx_duplication`  (
   `tx_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '事务id',
   `create_time` datetime(0) NULL DEFAULT NULL,
   PRIMARY KEY (`tx_no`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '事务记录表（去重表），用于幂等控制' ROW_FORMAT = Dynamic;

```

* 2、新建bank2服务对应的数据库distributed-tx-msg-bank2，执行如下sql：

```sql
create database distributed-tx-msg-bank2 character set utf8mb4;

use distributed-tx-msg-bank2;

DROP TABLE IF EXISTS `t_account`;
CREATE TABLE `t_account`  (
    `id` bigint(0) NOT NULL,
    `account` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL comment '账户名',
    `balance` decimal(5, 2) NOT NULL comment '账户余额',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic comment='账户表';

INSERT INTO `t_account`(`id`, `account`, `balance`) VALUES (2, 'lisi', 0.00);

DROP TABLE IF EXISTS `tx_duplication`;
CREATE TABLE `tx_duplication`  (
   `tx_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '事务id',
   `create_time` datetime(0) NULL DEFAULT NULL,
   PRIMARY KEY (`tx_no`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '事务记录表（去重表），用于幂等控制' ROW_FORMAT = Dynamic;

```

其中在每个数据库中都额外新增一张表tx_duplication，用于记录事务幂等控制。



#### 8.4.3、添加RocketMQ依赖

```xml
<!-- https://mvnrepository.com/artifact/org.apache.rocketmq/rocketmq-spring-boot-starter -->
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-spring-boot-starter</artifactId>
    <version>2.2.0</version>
</dependency>
```

##### 8.4.3.1、RocketMQ版本不兼容问题（了解）

关于 `rocketmq-spring-boot-starter` 的版本问题，小于2.1.0之前的版本，在事务消息处有几个重要的改动：

* **RocketMQTemplate**#sendMessageInTransaction的参数从4个变成了3个，去除了txProducerGroup：

  2.1.0之前：
```java
/**
     * Send Spring Message in Transaction
     *
     * @param txProducerGroup the validate txProducerGroup name, set null if using the default name
     * @param destination     destination formats: `topicName:tags`
     * @param message         message {@link org.springframework.messaging.Message}
     * @param arg             ext arg
     * @return TransactionSendResult
     * @throws MessagingException
     */
public TransactionSendResult sendMessageInTransaction(final String txProducerGroup, final String destination, final Message<?> message, final Object arg) throws MessagingException {
    try {
        TransactionMQProducer txProducer = this.stageMQProducer(txProducerGroup);
        org.apache.rocketmq.common.message.Message rocketMsg = RocketMQUtil.convertToRocketMessage(objectMapper,
                                                                                                   charset, destination, message);
        return txProducer.sendMessageInTransaction(rocketMsg, arg);
    } catch (MQClientException e) {
        throw RocketMQUtil.convert(e);
    }
}
```

​		2.1.0及其之后：

  ```java
  /**
       * Send Spring Message in Transaction
       *
       * @param destination destination formats: `topicName:tags`
       * @param message message {@link org.springframework.messaging.Message}
       * @param arg ext arg
       * @return TransactionSendResult
       * @throws MessagingException
       */
  public TransactionSendResult sendMessageInTransaction(final String destination,
                                                        final Message<?> message, final Object arg) throws MessagingException {
      try {
          if (((TransactionMQProducer) producer).getTransactionListener() == null) {
              throw new IllegalStateException("The rocketMQTemplate does not exist TransactionListener");
          }
          org.apache.rocketmq.common.message.Message rocketMsg = this.createRocketMqMessage(destination, message);
          return producer.sendMessageInTransaction(rocketMsg, arg);
      } catch (MQClientException e) {
          throw RocketMQUtil.convert(e);
      }
  }
  ```

* **@RocketMQTransactionListener**注解中也同样移除了txProducerGroup：

2.1.0之前：

```java
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface RocketMQTransactionListener {
 
    /**
     * Set ExecutorService params -- corePoolSize
     */
    int corePoolSize() default 1;
 
    /**
     * Set ExecutorService params -- maximumPoolSize
     */
    int maximumPoolSize() default 1;
 
    /**
     * Set ExecutorService params -- keepAliveTime
     */
    long keepAliveTime() default 1000 * 60; //60ms
 
    /**
     * Set ExecutorService params -- blockingQueueSize
     */
    int blockingQueueSize() default 2000;
 
    /**
     * Set rocketMQTemplate bean name, the default is rocketMQTemplate.
     * if use ExtRocketMQTemplate, can set ExtRocketMQTemplate bean name.
     */
    String rocketMQTemplateBeanName() default "rocketMQTemplate";
    
    String txProducerGroup();
 
}
```

2.1.0及其之后：

```java
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface RocketMQTransactionListener {
 
    /**
     * Set ExecutorService params -- corePoolSize
     */
    int corePoolSize() default 1;
 
    /**
     * Set ExecutorService params -- maximumPoolSize
     */
    int maximumPoolSize() default 1;
 
    /**
     * Set ExecutorService params -- keepAliveTime
     */
    long keepAliveTime() default 1000 * 60; //60ms
 
    /**
     * Set ExecutorService params -- blockingQueueSize
     */
    int blockingQueueSize() default 2000;
 
    /**
     * Set rocketMQTemplate bean name, the default is rocketMQTemplate.
     * if use ExtRocketMQTemplate, can set ExtRocketMQTemplate bean name.
     */
    String rocketMQTemplateBeanName() default "rocketMQTemplate";
 
}
```

在rocketmq-spring-boot-starter < 2.1.0以前的项目中，可以使用多个@RocketMQTransactionListener来监听不同的txProducerGroup来发送不同类型的事务消息到topic， 但是现在在一个项目中，如果你在一个project中写了多个@RocketMQTransactionListener，项目将不能启动，启动会报

```text
java.lang.IllegalStateException: rocketMQTemplate already exists RocketMQLocalTransactionListener
```

具体可以看源码**RocketMQTransactionConfiguration**：

```java
@Configuration
public class RocketMQTransactionConfiguration implements ApplicationContextAware, SmartInitializingSingleton {

    private final static Logger log = LoggerFactory.getLogger(RocketMQTransactionConfiguration.class);

    private ConfigurableApplicationContext applicationContext;

    @Override public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = (ConfigurableApplicationContext) applicationContext;
    }

    @Override public void afterSingletonsInstantiated() {
        Map<String, Object> beans = this.applicationContext.getBeansWithAnnotation(RocketMQTransactionListener.class)
            .entrySet().stream().filter(entry -> !ScopedProxyUtils.isScopedTarget(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        beans.forEach(this::registerTransactionListener);
    }

    private void registerTransactionListener(String beanName, Object bean) {
        Class<?> clazz = AopProxyUtils.ultimateTargetClass(bean);

        if (!RocketMQLocalTransactionListener.class.isAssignableFrom(bean.getClass())) {
            throw new IllegalStateException(clazz + " is not instance of " + RocketMQLocalTransactionListener.class.getName());
        }
        RocketMQTransactionListener annotation = clazz.getAnnotation(RocketMQTransactionListener.class);
        RocketMQTemplate rocketMQTemplate = (RocketMQTemplate) applicationContext.getBean(annotation.rocketMQTemplateBeanName());
        if (((TransactionMQProducer) rocketMQTemplate.getProducer()).getTransactionListener() != null) {
            throw new IllegalStateException(annotation.rocketMQTemplateBeanName() + " already exists RocketMQLocalTransactionListener");
        }
        ((TransactionMQProducer) rocketMQTemplate.getProducer()).setExecutorService(new ThreadPoolExecutor(annotation.corePoolSize(), annotation.maximumPoolSize(),
            annotation.keepAliveTime(), TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(annotation.blockingQueueSize())));
        ((TransactionMQProducer) rocketMQTemplate.getProducer()).setTransactionListener(RocketMQUtil.convert((RocketMQLocalTransactionListener) bean));
        log.debug("RocketMQLocalTransactionListener {} register to {} success", clazz.getName(), annotation.rocketMQTemplateBeanName());
    }
}
```

也就是说项目中只能有一个@RocketMQTransactionListener, 不能出现多个，所以去除了txProducerGroup。

在客户端，首先用户需要实现RocketMQLocalTransactionListener接口，并在接口类上注解声明 @RocketMQTransactionListener，实现确认和回查方法；然后再使用资源模板 RocketMQTemplate， 调用方法sendMessageInTransaction()来进行消息的发布。 注意：从 RocketMQ-Spring 2.1.0版本之后，注解@RocketMQTransactionListener不能设置 txProducerGroup、ak、sk，这些值均与对应的RocketMQTemplate保持一致。

RocketMQTemplate的初始化可以看RocketMQ的自动配置类**RocketMQAutoConfiguration**，此处忽略。




#### 8.4.4、修改项目配置文件

##### 8.4.4.1、bank1的配置文件

```yaml
server:
  port: 8081

# 实际虚拟机地址
server-ip: 192.168.213.130

nacos-server: ${server-ip}:8848

spring:
  application:
    name: distributed-tx-msg-bank1
  cloud:
    nacos:
      config:
        server-addr: ${nacos-server}
        username: ${nacos-username:nacos}
        password: ${nacos-password:nacos}
        namespace: ${nacos-namespace:public}
        file-extension: yml
        extension-configs:
          - dataId: common_datasource.yml
            refresh: true
        enabled: true # 启用nacos配置，默认为true
        max-retry: 5
        config-long-poll-timeout: 30000
      discovery:
        username: ${nacos-username:nacos}
        password: ${nacos-password:nacos}
        server-addr: ${nacos-server}
        namespace: ${nacos-namespace:public}
#        register-enabled: true # 是否注册，默认true（注册）
#        enabled: true # 启用服务发现功能， 默认为true

# 防止nacos狂刷
logging:
  level:
    com.alibaba.nacos.client: error

ribbon:
  ConnectTimeout: 3000
  ReadTimeout: 6000
   
rocketmq:
  name-server: ${server-ip}:9876
  producer:
    group: producer_bank1 # 生产者的组

```
其余配置项都存放在Nacos上：

common_datasource.yml

```yaml
mysql:
    host: 192.168.213.130
    username: caychen
    password: 1qaz@WSX
```

distributed-tx-msg-bank1.yml

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://${mysql.host}:3306/distributed-tx-msg-bank1?useSSL=false&useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&allowMultiQueries=true&serverTimezone=Asia/Shanghai
    username: ${mysql.username}
    password: ${mysql.password}
    hikari:
      # 最小空闲连接数量
      minimum-idle: 5
      # 空闲连接存活最大时间，默认600000（10分钟）
      idle-timeout: 180000
      # 连接池最大连接数，默认是10
      maximum-pool-size: 10
      # 此属性控制从池返回的连接的默认自动提交行为,默认值：true
      auto-commit: true
      # 连接池名称
      pool-name: MyHikariCP
      # 此属性控制池中连接的最长生命周期，值0表示无限生命周期，默认1800000即30分钟
      max-lifetime: 1800000
      # 数据库连接超时时间,默认30秒，即30000
      connection-timeout: 30000
      connection-test-query: SELECT 1
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: GMT+8

mybatis-plus:
  configuration:
    # 驼峰下划线转换
    map-underscore-to-camel-case: true
    auto-mapping-behavior: full
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  mapper-locations: classpath*:mapper/**/*Mapper.xml
  global-config:
    # 逻辑删除配置
    db-config:
      # 删除前
      logic-not-delete-value: 1
      # 删除后
      logic-delete-value: 0
  type-aliases-package: com.caychen.seata.bank.entity
```



##### 8.4.4.2、bank2的配置文件

同bank1类似， 除了如下：

```yaml
server:
  # 端口不一样
  port: 8082

spring:
  application:
    # 项目名不一样
    name: distributed-tx-msg-bank2
    
  datasource:
    # 数据库名不一样
    url: jdbc:mysql://${mysql.host}:3306/distributed-tx-msg-bank2?useSSL=false&useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&allowMultiQueries=true&serverTimezone=Asia/Shanghai
```



#### 8.4.5、业务逻辑代码

##### 8.4.5.1、bank1部分代码

（1）、用于自动填充的配置类

```java
@Component
@Slf4j
public class AutoFillConfig implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        log.info("start insert fill ....");
        this.strictInsertFill(metaObject, "createTime", Date.class, new Date()); // 起始版本 3.3.0(推荐使用)
    }

    @Override
    public void updateFill(MetaObject metaObject) {
    }
}

```

（2）、bank1生产者的topic信息

```java
@Data
@ConfigurationProperties("bank1.producer")
public class TopicProperties {

    private String topic;

    private String tag;
}
```

（3）、请求交互类

```java
@Data
public class TransferRequest {

    @NotNull
    private Long fromId;

    @NotNull
    private Long toId;

    @NotNull
    private BigDecimal money;

    private String txNo;
}
```

（4）、实体类

```java
@Data
@TableName(value = "t_account")
public class Account {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String account;

    private BigDecimal balance;
}

@Data
@TableName(value = "tx_duplication")
public class TxDuplication {

    @TableId(type = IdType.INPUT)
    @TableField("tx_no")
    private String txNo;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

}
```

（5）、Dao类

```java
@Mapper
public interface ITxDuplicationMapper extends BaseMapper<TxDuplication> {
}

@Mapper
public interface IAccountMapper extends BaseMapper<Account> {
}
```

（6）、业务接口及实现类

```java
public interface ITransferService {

    /**
     * 转账前的准备，即发送mq消息
     *
     * @param transferRequest
     * @return
     * @throws Exception
     */
    Boolean transfer(TransferRequest transferRequest) throws Exception;

    /**
     * 更新账户，扣减金额
     *
     * @param transferRequest
     * @return
     * @throws Exception
     */
    Boolean doTransfer(TransferRequest transferRequest) throws Exception;
}
```

```java
@Slf4j
@Service
@EnableConfigurationProperties(TopicProperties.class)
public class TransferServiceImpl implements ITransferService {

    @Autowired
    private IAccountMapper accountMapper;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private TopicProperties topicProperties;

    @Autowired
    private ITxDuplicationMapper txDuplicationMapper;

    /**
     * 转账前的准备，即发送mq消息
     *
     * @param transferRequest
     * @return
     * @throws Exception
     */
    @Override
    public Boolean transfer(TransferRequest transferRequest) {
        //发送mq的事务消息
        Message message = MessageBuilder.withPayload(JSON.toJSONString(transferRequest)).build();

        /**
         * @param destination destination formats: `topicName:tags`
         * @param message message {@link org.springframework.messaging.Message}
         * @param arg ext arg
         *
         */
        String destination = topicProperties.getTopic();
        rocketMQTemplate.sendMessageInTransaction(destination, message, null);
        return Boolean.TRUE;
    }

    /**
     * 执行本地事务：更新账户，扣减金额
     *
     * @param transferRequest
     * @return
     * @throws Exception
     */
    @Override
    @Transactional
    public Boolean doTransfer(TransferRequest transferRequest) throws Exception {
        log.info("开始更新bank1的账户信息...");
        String txNo = transferRequest.getTxNo();
        //判断幂等性
        Integer count = txDuplicationMapper.selectCount(
                new LambdaQueryWrapper<TxDuplication>()
                        .eq(TxDuplication::getTxNo, txNo)
        );

        //如果count==0，则执行本地事务
        if (count == 0) {
            Account account = accountMapper.selectById(transferRequest.getFromId());
            BigDecimal balance = account.getBalance();

            //检查余额
            if (balance.compareTo(transferRequest.getMoney()) >= 0) {
                account.setBalance(balance.subtract(transferRequest.getMoney()));
                accountMapper.updateById(account);

                //保存事务id
                TxDuplication txDuplication = new TxDuplication();
                txDuplication.setTxNo(txNo);
                txDuplicationMapper.insert(txDuplication);

                log.info("完成bank1账户更新操作...");
                return Boolean.TRUE;
            } else {
                log.error("余额不足，无法转账");
                throw new Exception("余额不足，无法转账");
            }
        } else {
            //否则直接返回
            log.warn("无须操作...");
            return Boolean.FALSE;
        }
    }

}
```

（7）、Controller控制器类

```java
@RestController
@RequestMapping("/v1/bank1/transfer")
public class TransferController {

    @Autowired
    private ITransferService transferService;

    @PostMapping
    public String transferMoney(@RequestBody @Validated TransferRequest transferRequest) throws Exception {
        transferRequest.setTxNo(UUID.randomUUID().toString().replace("-", ""));
        Boolean transferFlag = transferService.transfer(transferRequest);
        return transferFlag ? "success" : "fail";
    }
}
```

（8）、最重要的：事务消息监听器

```java
@Slf4j
@Component
@RocketMQTransactionListener
public class ProducerTxmsgListener implements RocketMQLocalTransactionListener {

    @Autowired
    private ITransferService transferService;

    @Autowired
    private ITxDuplicationMapper txDuplicationMapper;

    /**
     * RocketMQ发送者发送事务消息之后的回调
     *
     * @param msg
     * @param arg
     * @return
     */
    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        log.info("开始事务消息回调");
        try {
            String messageString = new String((byte[]) msg.getPayload());
            TransferRequest transferRequest = JSON.parseObject(messageString, TransferRequest.class);
            log.info("消息回调txNo: [{}]", transferRequest.getTxNo());
            
            transferService.doTransfer(transferRequest);

            //模拟三种状态的场景
            BigDecimal money = transferRequest.getMoney();
            if (money.compareTo(new BigDecimal("5")) == 0) {
                //提交commit
                //正常返回，则进行commit，自动向mq发送commit消息，mq消息的状态则变为可消费
                return RocketMQLocalTransactionState.COMMIT;
            } else if (money.compareTo(new BigDecimal("10")) == 0) {
                //提交unknown，事务回查
                return RocketMQLocalTransactionState.UNKNOWN;
            } else if (money.compareTo(new BigDecimal("20")) == 0) {
                //提交rollback，消息删除
                return RocketMQLocalTransactionState.ROLLBACK;
            } else {
                //其他提交commit
                return RocketMQLocalTransactionState.COMMIT;
            }
        } catch (Exception e) {
            log.error("发生异常：", e);
            //异常返回，进行rollback，自动向mq发送rollback消息，mq消息则被删除
            return RocketMQLocalTransactionState.ROLLBACK;
        }

    }

    /**
     * 事务回查
     *
     * @param msg
     * @return
     */
    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        String messageString = new String((byte[]) msg.getPayload());
        TransferRequest transferRequest = JSON.parseObject(messageString, TransferRequest.class);
        log.info("回查txNo: [{}]", transferRequest.getTxNo());

        //判断幂等性
        Integer count = txDuplicationMapper.selectCount(
                new LambdaQueryWrapper<TxDuplication>()
                        .eq(TxDuplication::getTxNo, transferRequest.getTxNo())
        );
        if (count > 0) {
            //如果查询到，则发送commit
            return RocketMQLocalTransactionState.COMMIT;
        }

        //如果未查询到，则发送unknown
        return RocketMQLocalTransactionState.UNKNOWN;
    }
}
```



##### 8.4.5.2、bank2部分代码

（1）、AutoFillConfig，IAccountMapper，ITxDuplicationMapper，TransferRequest，Account，TxDuplication同bank1一模一样，此处就不粘贴了。

（2）、业务接口及其实现类

```java
public interface IAccountService {

    /**
     * 新增账户金额
     *
     * @param transferRequest
     */
    void addAccountInfoBalance(TransferRequest transferRequest) throws Exception;
}
```

```java
@Slf4j
@Service
public class AccountServiceImpl implements IAccountService {

    @Autowired
    private IAccountMapper accountMapper;

    @Autowired
    private ITxDuplicationMapper txDuplicationMapper;

    /**
     * 增加账户金额
     *
     * @param transferRequest
     * @throws Exception
     */
    @Override
    @Transactional
    public void addAccountInfoBalance(TransferRequest transferRequest) throws Exception {
        log.info("开始更新bank2的账户信息...");
        String txNo = transferRequest.getTxNo();

        //判断幂等性
        Integer count = txDuplicationMapper.selectCount(
                new LambdaQueryWrapper<TxDuplication>()
                        .eq(TxDuplication::getTxNo, txNo)
        );

        if (count > 0) {
            log.warn("无须操作...");
            return;
        }

        Account account = accountMapper.selectById(transferRequest.getToId());
        if (account == null) {
            log.error("账户信息不存在...");
            throw new Exception("账户信息不存在...");
        }

        account.setBalance(account.getBalance().add(transferRequest.getMoney()));
        accountMapper.updateById(account);

        TxDuplication txDuplication = new TxDuplication();
        txDuplication.setTxNo(txNo);
        txDuplicationMapper.insert(txDuplication);
        log.info("完成bank2账户更新操作...");
    }
}
```

（3）、订阅方的消息消费逻辑

```java
@Slf4j
@Component
@RocketMQMessageListener(topic = "${bank2.consumer.topic}", consumerGroup = "${rocketmq.consumer.group}")
public class Bank2RocketMessageConsumer implements RocketMQListener<String> {

    @Autowired
    private IAccountService accountService;

    @Override
    public void onMessage(String message) {
        log.info("bank2接收到消息：[{}]", message);

        TransferRequest transferRequest = JSON.parseObject(message, TransferRequest.class);
        log.info("消费txNo: [{}]", transferRequest.getTxNo());

        try {
            accountService.addAccountInfoBalance(transferRequest);
            log.info("bank2消费成功...");
        } catch (Exception e) {
            e.printStackTrace();
            log.error("bank2消费失败...");
        }
    }
}
```



#### 8.4.6、测试场景

##### 8.4.6.1、正常流程：

```
POST http://localhost:8081/v1/bank1/transfer
Accept: */*
Content-Type: application/json;charset=utf-8

{
  "toId": 2,
  "fromId": 1,
  "money": 20
}
```

bank1的余额情况如图：

![](./images/msg之账户1的余额情况.png)

bank1的事务情况如图：

![](./images/msg之账户1的事务情况.png)

bank2的余额情况如图：

![](./images/msg之账户2的余额情况.png)

bank2的事务情况如图：

![](./images/msg之账户2的事务情况.png)

在正常流程的情况下，bank1库的tx_duplication表中的数据和bank2库的tx_duplication表中的数据基本一致，只是时间不同而已。



##### 8.4.6.2、bank1的异常场景

```text
POST http://localhost:8081/v1/bank1/transfer
Accept: */*
Content-Type: application/json;charset=utf-8

{
  "toId": 2,
  "fromId": 1,
  "money": 100
}
```

使用上述请求，转账金额超过bank1中用户的余额，则转账失败，bank1的本地事务被回滚，同时MQ消息则被删除，bank2就消费不到这条消息。



##### 8.4.6.3、bank2的异常场景

```text
POST http://localhost:8081/v1/bank1/transfer
Accept: */*
Content-Type: application/json;charset=utf-8

{
  "toId": 3,
  "fromId": 1,
  "money": 5
}
```

使用上述请求，bank1能正常提交本地事务，bank1扣减金额正常，bank1的业务流程完毕；而bank2在消费消息的时候，由于toId对应的账户信息不存在，导致消息消费失败，则会一直重试，直到消费成功为止。

bank1正常执行业务之后的余额如图：

![](./images/msg之bank1正常执行业务之后的余额.png)

bank1正常执行业务之后的事务如图：

![](./images/msg之bank1正常执行业务之后的事务.png)

bank2异常消费之后的余额如图：

![](./images/msg之bank2异常消费之后的余额.png)

bank2异常消费之后的事务如图：

![](./images/msg之bank2异常消费之后的事务.png)



可以看出来，两个账户的余额总数少了5，同时bank1的事务记录数比bank2的事务记录数多一条。



##### 8.4.6.4、事务回查

```text
POST http://localhost:8081/v1/bank1/transfer
Accept: */*
Content-Type: application/json;charset=utf-8

{
  "toId": 2,
  "fromId": 1,
  "money": 10
}
```

由于money=10，会提交unknown，则mq会定时进行回查。

![](./images/模拟事务回查.png)

过了一会儿，断点就会停留在回查方法里。

![](./images/事务消息的回查机制.png)








## 9、分布式事务解决方案之最大努力通知

最大努力通知的方案实现比较简单，适用于一些最终一致性要求较低的业务。

执行流程：

- 系统 A 本地事务执行完之后，发送个消息到 MQ；
- 这里会有个专门消费 MQ 的服务，这个服务会消费 MQ 并调用系统 B 的接口；
- 要是系统 B 执行成功就 ok 了；要是系统 B 执行失败了，那么最大努力通知服务就定时尝试重新调用系统 B, 反复 N 次，最后还是不行就放弃。



## 总结

