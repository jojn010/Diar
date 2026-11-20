-- Run in Supabase SQL Editor after the favorites migration, with at least one listing.
-- All fixture changes are rolled back. No accounts or committed listings are created.
begin;
select set_config('request.jwt.claim.sub', (select owner_id::text from public.listings order by id limit 1), true);
set local role authenticated;
do $$
declare home uuid; owner uuid;
begin
  select id,owner_id into home,owner from public.listings order by id limit 1;
  if home is null then raise exception 'RLS test requires one existing listing'; end if;
  insert into public.favorites(user_id,listing_id) values(owner,home) on conflict do nothing;
  if not exists(select 1 from public.favorites where user_id=owner and listing_id=home) then
    raise exception 'Owner cannot read their own favorite';
  end if;
  perform set_config('request.jwt.claim.sub','00000000-0000-4000-8000-000000000099',true);
  if exists(select 1 from public.favorites) then raise exception 'Another account can read private favorites'; end if;
  delete from public.favorites where user_id=owner and listing_id=home;
  if found then raise exception 'Another account can delete private favorites'; end if;
  begin
    insert into public.favorites(user_id,listing_id) values(owner,home);
    raise exception 'Forged owner was accepted';
  exception when insufficient_privilege then null;
  end;
  perform set_config('request.jwt.claim.sub',owner::text,true);
  delete from public.favorites where user_id=owner and listing_id=home;
  if not found then raise exception 'Owner cannot remove their favorite'; end if;
end $$;
reset role;
rollback;
select 'owner read/write and cross-account isolation passed; fixtures rolled back' as favorites_rls_result;
