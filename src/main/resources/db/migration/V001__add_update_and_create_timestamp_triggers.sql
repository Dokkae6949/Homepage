create or replace function handle_timestamps()
    returns trigger as
$$
begin
    if lower(tg_op) = 'insert' then
        new.created_at = coalesce(new.created_at, current_timestamp);
        new.updated_at = coalesce(new.updated_at, current_timestamp);
    elsif lower(tg_op) = 'update' then
        if new.created_at is distinct from old.created_at then
            raise exception 'Direct modification of created_at is not allowed.';
        end if;

        new.updated_at = current_timestamp;
    end if;

    return new;
end;
$$ language plpgsql;