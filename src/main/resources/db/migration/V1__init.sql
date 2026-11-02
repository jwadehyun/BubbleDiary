-- The starting schema. Never edit a migration that has run somewhere; add V2__..., V3__... instead.

create table users (
    user_id         bigint       not null auto_increment,
    provider        varchar(255) not null,
    provider_id     varchar(255) not null,
    email           varchar(255),
    name            varchar(255),
    display_name    varchar(255) not null,
    fandom          varchar(255) not null,
    avatar          longtext,
    status_text     varchar(60),
    profile_message varchar(150),
    voice           enum ('ENGLISH','KOREAN','MIXED') not null,
    fan_prompt      varchar(300),
    plan            enum ('FREE','PREMIUM') not null,
    primary key (user_id),
    constraint uk_users_provider unique (provider, provider_id)
) engine = InnoDB;

create table messages (
    message_id bigint      not null auto_increment,
    user_id    bigint      not null,
    text       text,
    image      longblob,
    image_type varchar(255),
    created_at datetime(6) not null,
    status     enum ('DONE','ERROR','PENDING') not null,
    error      varchar(1000),
    primary key (message_id),
    constraint fk_messages_user foreign key (user_id) references users (user_id)
) engine = InnoDB;

-- A user's posts in order, and the daily post count.
create index idx_messages_user_created on messages (user_id, created_at);

create table replies (
    reply_id   bigint       not null auto_increment,
    message_id bigint       not null,
    fan        varchar(255) not null,
    text       varchar(300) not null,
    created_at datetime(6)  not null,
    likes      integer      not null,
    ai         bit          not null,
    kind       enum ('ANTI','FAN') not null,
    primary key (reply_id),
    constraint fk_replies_message foreign key (message_id) references messages (message_id) on delete cascade
) engine = InnoDB;

create index idx_replies_message on replies (message_id);

create table refresh_tokens (
    id         bigint      not null auto_increment,
    user_id    bigint      not null,
    token_hash varchar(64) not null,
    expires_at datetime(6) not null,
    primary key (id),
    constraint uk_refresh_tokens_hash unique (token_hash),
    constraint fk_refresh_tokens_user foreign key (user_id) references users (user_id)
) engine = InnoDB;
