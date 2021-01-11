CREATE TABLE IF NOT EXISTS todo (
    user_id varchar(32),
    todo_id varchar(36) primary key,
    todo_title varchar(30),
    finished boolean,
    created_at timestamp,
    version bigint
);
