-- =============================================
-- 1. 创建邮件系统数据库
-- =============================================
IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = 'mail_system')
BEGIN
    CREATE DATABASE mail_system;
END
GO

USE mail_system;
GO

-- =============================================
-- 2. 系统用户表（sys_user）
-- =============================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'sys_user')
BEGIN
    CREATE TABLE sys_user (
        user_id BIGINT IDENTITY(1,1) PRIMARY KEY, -- 系统用户ID（主键）
        username VARCHAR(50) NOT NULL, -- 登录用户名
        password VARCHAR(100) NOT NULL, -- 密码（加密存储）
        nickname NVARCHAR(50) NOT NULL, -- 用户昵称（支持中文）
        phone VARCHAR(20) NULL, -- 手机号
        email VARCHAR(100) NULL, -- 系统登录邮箱（非邮件账户）
        status TINYINT NOT NULL DEFAULT 1, -- 状态：0-禁用，1-正常
        is_deleted TINYINT NOT NULL DEFAULT 0, -- 软删除：0-未删，1-已删
        create_time DATETIME2 NOT NULL DEFAULT GETDATE(), -- 创建时间（高精度）
        update_time DATETIME2 NOT NULL DEFAULT GETDATE(), -- 更新时间
        -- 唯一约束：用户名不重复
        CONSTRAINT UK_sys_user_username UNIQUE (username)
    );

    -- 索引：优化登录查询
    CREATE INDEX idx_sys_user_username ON sys_user(username);
END
GO
select* from sys_user
-- =============================================
-- 3. 邮箱账户表（mail_account）
-- =============================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'mail_account')
BEGIN
    CREATE TABLE mail_account (
        account_id BIGINT IDENTITY(1,1) PRIMARY KEY, -- 邮箱账户ID（主键）
        user_id BIGINT NOT NULL, -- 关联系统用户ID
        email VARCHAR(100) NOT NULL, -- 邮箱地址（唯一，用于外键关联）
        protocol_smtp VARCHAR(50) NOT NULL, -- SMTP服务器地址
        protocol_smtp_port INT NOT NULL, -- SMTP端口
        protocol_pop3 VARCHAR(50) NOT NULL, -- POP3服务器地址
        protocol_pop3_port INT NOT NULL, -- POP3端口
        auth_code VARCHAR(100) NOT NULL, -- 邮箱授权码（直接包含在创建语句中，无需后续ALTER）
        is_default TINYINT NOT NULL DEFAULT 1, -- 是否默认邮箱：0-否，1-是
        is_deleted TINYINT NOT NULL DEFAULT 0, -- 软删除
        create_time DATETIME2 NOT NULL DEFAULT GETDATE(),
        update_time DATETIME2 NOT NULL DEFAULT GETDATE(),
        -- 外键约束：关联系统用户（用户删除时级联删除邮箱账户）
        CONSTRAINT FK_mail_account_sys_user FOREIGN KEY (user_id) 
            REFERENCES sys_user(user_id) ON DELETE CASCADE,
        -- 唯一约束：邮箱地址不重复（用于mail_message的外键关联）
        CONSTRAINT UK_mail_account_email UNIQUE (email)
    );

    -- 索引：优化查询性能
    CREATE INDEX idx_mail_account_user_id ON mail_account(user_id);
    CREATE INDEX idx_mail_account_email ON mail_account(email);
END
GO
ALTER TABLE mail_account
ADD 
    protocol_imap VARCHAR(50) NOT NULL DEFAULT '' -- IMAP服务器地址（非空，默认空字符串，后续插入时覆盖）,、
ALTER TABLE mail_account
ADD 
    protocol_imap_port INT NOT NULL DEFAULT 0    -- IMAP端口（非空，默认0，后续插入时覆盖）
GO

DELETE FROM mail_account
select* from mail_account
-- =============================================
-- 4. 邮件文件夹表（mail_folder）
-- =============================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'mail_folder')
BEGIN
    CREATE TABLE mail_folder (
        folder_id BIGINT IDENTITY(1,1) PRIMARY KEY, -- 文件夹ID（主键）
        user_id BIGINT NOT NULL, -- 关联系统用户ID
        folder_name NVARCHAR(50) NOT NULL, -- 文件夹名称
        folder_type TINYINT NOT NULL, -- 类型：1-收件箱，2-发件箱，3-草稿箱，4-已删除，5-自定义
        sort_num INT NOT NULL DEFAULT 0, -- 排序序号
        is_deleted TINYINT NOT NULL DEFAULT 0, -- 软删除
        create_time DATETIME2 NOT NULL DEFAULT GETDATE(),
        update_time DATETIME2 NOT NULL DEFAULT GETDATE(),
        -- 外键约束：关联系统用户（用户删除时级联删除文件夹）
        CONSTRAINT FK_mail_folder_sys_user FOREIGN KEY (user_id) 
            REFERENCES sys_user(user_id) ON DELETE CASCADE,
        -- 唯一约束：同一用户的文件夹名称不重复
        CONSTRAINT UK_mail_folder_user_name UNIQUE (user_id, folder_name)
    );

    -- 索引：优化按用户/类型查询
    CREATE INDEX idx_mail_folder_user_id ON mail_folder(user_id);
    CREATE INDEX idx_mail_folder_user_type ON mail_folder(user_id, folder_type);
END
GO

-- =============================================
-- 5. 邮件主表（mail_message）
-- =============================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'mail_message')
BEGIN
    CREATE TABLE mail_message (
        message_id BIGINT IDENTITY(1,1) PRIMARY KEY, -- 邮件ID（主键）
        sender_account_email VARCHAR(100) NOT NULL, -- 发件人邮箱账户（直接创建，关联mail_account.email）
        sender_email VARCHAR(100) NOT NULL, -- 收件人显示邮箱（可能与账户邮箱一致，注释修正）
        subject NVARCHAR(255) NOT NULL, -- 邮件主题
        content NVARCHAR(MAX) NOT NULL, -- 邮件内容（HTML/纯文本）
        send_time DATETIME2 NULL, -- 发送时间（草稿为NULL，已发送为具体时间）
        draft_time DATETIME2 NULL, -- 草稿保存时间（草稿为具体时间，已发送为NULL）
        status TINYINT NOT NULL DEFAULT 0, -- 状态：0-草稿，1-已发送，2-已接收，3-已撤回
        read_status TINYINT NOT NULL DEFAULT 0, -- 阅读状态：0-未读，1-已读
        is_deleted TINYINT NOT NULL DEFAULT 0, -- 软删除
        create_time DATETIME2 NOT NULL DEFAULT GETDATE(),
        update_time DATETIME2 NOT NULL DEFAULT GETDATE(),
        -- 外键约束：关联发件人邮箱账户（邮箱账户删除时不删除邮件，避免数据丢失）
        -- 核心修正：关联mail_account的email列（VARCHAR类型）
        CONSTRAINT FK_mail_message_sender FOREIGN KEY (sender_account_email) 
            REFERENCES mail_account(email) ON DELETE NO ACTION
    );
    ALTER TABLE dbo.mail_message
DROP CONSTRAINT FK_mail_message_sender;

-- 1. 给 mail_message 表添加 mail_uid 字段（存储唯一标识）
ALTER TABLE dbo.mail_message
ADD mail_uid VARCHAR(200) NULL; -- 长度200足够存储拼接后的唯一标识

-- 2. 添加唯一约束：同一邮件的 UID 只能存一次（避免重复插入）
ALTER TABLE dbo.mail_message
ADD CONSTRAINT UK_mail_message_uid UNIQUE (mail_uid);


select* from mail_message  order by send_time 
delete from mail_message 
    -- 索引：优化查询性能（使用新列名，避免旧列名失效）
    CREATE INDEX idx_mail_message_sender ON mail_message(sender_account_email);
    CREATE INDEX idx_mail_message_status ON mail_message(status);
    CREATE INDEX idx_mail_message_send_time ON mail_message(send_time);
    CREATE INDEX idx_mail_message_draft_time ON mail_message(draft_time);
END
GO

-- =============================================
-- 6. 邮件收件人表（mail_recipient）
-- =============================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'mail_recipient')
BEGIN
    CREATE TABLE mail_recipient (
        recipient_id BIGINT IDENTITY(1,1) PRIMARY KEY, -- 收件人ID（主键）
        message_id BIGINT NOT NULL, -- 关联邮件ID
        recipient_account_id BIGINT NOT NULL, -- 收件人邮箱账户ID（关联mail_account.account_id）
        recipient_email VARCHAR(100) NOT NULL, -- 收件人邮箱地址（修正拼写：recipent→recipient）
        recipient_type TINYINT NOT NULL, -- 类型：1-收件人，2-抄送，3-密送
        is_read TINYINT NOT NULL DEFAULT 0, -- 收件人是否已读：0-未读，1-已读
        create_time DATETIME2 NOT NULL DEFAULT GETDATE(),
        -- 外键约束
        CONSTRAINT FK_mail_recipient_message FOREIGN KEY (message_id) 
            REFERENCES mail_message(message_id) ON DELETE CASCADE,
        CONSTRAINT FK_mail_recipient_account FOREIGN KEY (recipient_account_id) 
            REFERENCES mail_account(account_id) ON DELETE NO ACTION
    );

    ALTER TABLE dbo.mail_recipient
    DROP CONSTRAINT FK_mail_recipient_account; 

    ALTER TABLE dbo.mail_recipient
ALTER COLUMN recipient_account_id BIGINT NULL;


    -- 索引：优化查询
    CREATE INDEX idx_mail_recipient_message ON mail_recipient(message_id);
    CREATE INDEX idx_mail_recipient_recipient ON mail_recipient(recipient_account_id);
    CREATE INDEX idx_mail_recipient_email ON mail_recipient(recipient_email); -- 新增邮箱索引
END
GO


delete from mail_recipient
select* from mail_recipient
-- =============================================
-- 7. 邮件文件夹关联表（mail_message_folder）
-- =============================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'mail_message_folder')
BEGIN
    CREATE TABLE mail_message_folder (
        id BIGINT IDENTITY(1,1) PRIMARY KEY, -- 关联ID（主键）
        message_id BIGINT NOT NULL, -- 关联邮件ID
        folder_id BIGINT NOT NULL, -- 关联文件夹ID
        create_time DATETIME2 NOT NULL DEFAULT GETDATE(),
        -- 外键约束
        CONSTRAINT FK_mmf_message FOREIGN KEY (message_id) 
            REFERENCES mail_message(message_id) ON DELETE CASCADE,
        CONSTRAINT FK_mmf_folder FOREIGN KEY (folder_id) 
            REFERENCES mail_folder(folder_id) ON DELETE CASCADE,
        -- 唯一约束：一封邮件在一个文件夹中仅存一条记录
        CONSTRAINT UK_mmf_message_folder UNIQUE (message_id, folder_id)
    );

    -- 索引：优化按文件夹查询邮件
    CREATE INDEX idx_mmf_folder_message ON mail_message_folder(folder_id, message_id);
END
GO

-- =============================================
-- 8. 邮件附件表（mail_attachment）
-- =============================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'mail_attachment')
BEGIN
    CREATE TABLE mail_attachment (
        attachment_id BIGINT IDENTITY(1,1) PRIMARY KEY, -- 附件ID（主键）
        message_id BIGINT NOT NULL, -- 关联邮件ID
        file_name NVARCHAR(255) NOT NULL, -- 附件名称
        file_path VARCHAR(500) NOT NULL, -- 附件存储路径
        file_size BIGINT NOT NULL, -- 附件大小（字节）
        file_type VARCHAR(50) NOT NULL, -- 附件类型（如pdf/docx/jpg）
        upload_time DATETIME2 NOT NULL DEFAULT GETDATE(),
        -- 外键约束：关联邮件（邮件删除时级联删除附件）
        CONSTRAINT FK_mail_attachment_message FOREIGN KEY (message_id) 
            REFERENCES mail_message(message_id) ON DELETE CASCADE
    );

  

    -- 索引：优化按邮件查询附件
    CREATE INDEX idx_mail_attachment_message ON mail_attachment(message_id);
END
GO
ALTER TABLE mail_attachment
DROP COLUMN file_size;
GO

  select* from mail_attachment
  delete from mail_attachment
-- =============================================
-- 9. 邮件联系人表（mail_contact）
-- =============================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'mail_contact')
BEGIN
    CREATE TABLE mail_contact (
        contact_id BIGINT IDENTITY(1,1) PRIMARY KEY, -- 联系人ID（主键）
        user_id BIGINT NOT NULL, -- 关联系统用户ID（联系人归属的用户）
        contact_name NVARCHAR(50) NOT NULL, -- 联系人姓名（支持中文）
        contact_email VARCHAR(100) NOT NULL, -- 联系人邮箱地址
        contact_group NVARCHAR(50) NULL DEFAULT '默认分组', -- 联系人分组（如“客户/同事/家人”）
        phone VARCHAR(20) NULL, -- 联系人手机号（可选）
        remark NVARCHAR(200) NULL, -- 联系人备注（可选）
        is_star TINYINT NOT NULL DEFAULT 0, -- 是否星标收藏：0-否，1-是
        is_deleted TINYINT NOT NULL DEFAULT 0, -- 软删除：0-未删，1-已删
        create_time DATETIME2 NOT NULL DEFAULT GETDATE(), -- 创建时间
        update_time DATETIME2 NOT NULL DEFAULT GETDATE(), -- 更新时间
        -- 外键约束：关联系统用户，用户删除时联系人级联删除
        CONSTRAINT FK_mail_contact_sys_user FOREIGN KEY (user_id) 
            REFERENCES sys_user(user_id) ON DELETE CASCADE,
        -- 唯一约束：同一用户下，联系人邮箱不能重复（避免重复添加）
        CONSTRAINT UK_mail_contact_user_email UNIQUE (user_id, contact_email)
    );

    -- 索引：优化查询性能（按用户/分组/星标/邮箱查询）
    CREATE INDEX idx_mail_contact_user_id ON mail_contact(user_id);
    CREATE INDEX idx_mail_contact_user_group ON mail_contact(user_id, contact_group);
    CREATE INDEX idx_mail_contact_user_star ON mail_contact(user_id, is_star);
    CREATE INDEX idx_mail_contact_contact_email ON mail_contact(contact_email);
END
GO

PRINT '邮件系统数据库创建完成！';
GO



--drop table mail_contact
--drop table mail_attachment
--drop table mail_recipient
--drop table mail_folder
--drop table mail_account
--drop table mail_message
--drop table sys_user


USE mail_system;
GO
INSERT INTO sys_user (username, password, nickname, phone, email, status, is_deleted)
VALUES (
    'SuWeihan', -- 用户名
    'e10adc3949ba59abbe56e057f20f883e', -- 123456的MD5加密值
    'AWEI', -- 用户昵称
    '13187003557', -- 手机号
    '3540291485@qq.com', -- 系统登录邮箱（与邮件账户邮箱一致）
    1, -- 状态：正常
    0 -- 未删除
);
GO

INSERT INTO mail_account (
    user_id, email, protocol_smtp, protocol_smtp_port, 
    protocol_pop3, protocol_pop3_port, protocol_imap, protocol_imap_port, -- 新增IMAP字段
    auth_code, is_default, is_deleted
)
VALUES (
    1,
    '3540291485@qq.com', -- 邮箱地址
    'smtp.qq.com',       -- QQ邮箱SMTP服务器
    465,                 -- QQ邮箱SMTP端口（SSL加密）
    'pop.qq.com',        -- QQ邮箱POP3服务器
    995,                 -- QQ邮箱POP3端口（SSL加密）
    'imap.qq.com',       -- QQ邮箱IMAP服务器（固定值）
    993,                 -- QQ邮箱IMAP端口（SSL加密，固定值）
    'hztmyzkobgnwchjc',  -- 授权码
    1,                   -- 设为默认邮箱
    0                    -- 未删除
);
GO

INSERT INTO dbo.mail_account (
    user_id,
    email,
    protocol_smtp,       -- 163 邮箱 SMTP 服务器
    protocol_smtp_port,  -- 163 邮箱 SMTP SSL 端口
    protocol_pop3,       -- 163 邮箱 POP3 服务器
    protocol_pop3_port,  -- 163 邮箱 POP3 SSL 端口
    protocol_imap,       -- 新增：163 邮箱 IMAP 服务器
    protocol_imap_port,  -- 新增：163 邮箱 IMAP SSL 端口
    auth_code,           -- 授权码
    is_default,          -- 是否默认邮箱
    is_deleted           -- 软删除
) VALUES (
    1,                                  -- user_id：需确保 sys_user 表存在 user_id=1 的记录
    'ipvr05@163.com',                   -- 邮箱地址（唯一约束，不可重复）
    'smtp.163.com',                     -- 163 邮箱 SMTP 服务器（固定值）
    465,                                -- 163 邮箱 SMTP SSL 端口（固定值）
    'pop.163.com',                      -- 163 邮箱 POP3 服务器（固定值）
    995,                                -- 163 邮箱 POP3 SSL 端口（固定值）
    'imap.163.com',                     -- 163 邮箱 IMAP 服务器（固定值）
    993,                                -- 163 邮箱 IMAP SSL 端口（固定值）
    'MCfyqfRr8abcAdgS',                 -- 用户提供的授权码
    0,                                  -- 设为默认邮箱
    0                                   -- 未删除
);
GO

