begin;
create table public.favorites (
  user_id uuid not null references auth.users(id) on delete cascade default auth.uid(),
  listing_id uuid not null references public.listings(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (user_id, listing_id)
);
create index favorites_listing_id_idx on public.favorites (listing_id);
alter table public.favorites enable row level security;
revoke all on public.favorites from anon, authenticated;
grant select, insert, delete on public.favorites to authenticated;
create policy "Users read own favorites" on public.favorites for select to authenticated using ((select auth.uid()) = user_id);
create policy "Users add own favorites" on public.favorites for insert to authenticated with check ((select auth.uid()) = user_id);
create policy "Users remove own favorites" on public.favorites for delete to authenticated using ((select auth.uid()) = user_id);
commit;
