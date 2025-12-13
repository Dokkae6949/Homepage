create table message (
    id uuid not null primary key,
    author varchar(31) not null,
    content varchar(255) not null,

    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz
);

create trigger handle_message_timestamps
    before insert or update on message
    for each row
    execute function handle_timestamps();