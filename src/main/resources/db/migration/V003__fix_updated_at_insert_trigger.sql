--- Dropping trigger to prevent them from being called while migrating data
drop trigger if exists handle_message_timestamps on message;

--- Migrate data
update message
set updated_at = null
where created_at = updated_at
  and updated_at is not null;

--- Replace trigger handler
create or replace function handle_timestamps()
    returns trigger as
$$
begin
    if lower(tg_op) = 'insert' then
        new.created_at = coalesce(new.created_at, current_timestamp);
        new.updated_at = null;
    elsif lower(tg_op) = 'update' then
        if new.created_at is distinct from old.created_at then
            raise exception 'Direct modification of created_at is not allowed.';
        end if;

        new.updated_at = current_timestamp;
    end if;

    return new;
end;
$$ language plpgsql;

--- Reinsert trigger
create trigger handle_message_timestamps
    before insert or update
    on message
    for each row
execute function handle_timestamps();