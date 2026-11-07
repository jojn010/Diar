begin;
create table public.listings (
  id uuid primary key,
  owner_id uuid not null references auth.users(id) on delete cascade default auth.uid(),
  title text not null check (char_length(title) between 2 and 160),
  location text not null check (char_length(location) between 1 and 300),
  area numeric(12,2) not null check (area > 0 and area <= 1000000000),
  price bigint not null check (price between 1 and 1000000000),
  description text not null default '' check (char_length(description) <= 5000),
  negotiable boolean not null default false,
  purpose text not null check (purpose in ('sale', 'rent')),
  type text not null check (type in ('apartment', 'house', 'villa')),
  floors integer not null check (floors between 1 and 3),
  latitude double precision not null check (latitude between -90 and 90),
  longitude double precision not null check (longitude between -180 and 180),
  location_source text not null check (location_source in ('map', 'current')),
  photo_path text not null default '',
  created_at timestamptz not null default now(),
  constraint owner_photo_path check (photo_path = '' or photo_path like owner_id::text || '/' || id::text || '/%')
);
create index listings_owner_created on public.listings(owner_id, created_at desc);
alter table public.listings enable row level security;
revoke all on public.listings from anon, authenticated;
grant select on public.listings to anon, authenticated;
grant insert, update, delete on public.listings to authenticated;
create policy "Published listings are visible" on public.listings for select to anon, authenticated using (true);
create policy "Owners create listings" on public.listings for insert to authenticated with check ((select auth.uid()) = owner_id);
create policy "Owners update listings" on public.listings for update to authenticated using ((select auth.uid()) = owner_id) with check ((select auth.uid()) = owner_id);
create policy "Owners delete listings" on public.listings for delete to authenticated using ((select auth.uid()) = owner_id);

-- Public photos are intentional: the app warns before publishing a listing.
insert into storage.buckets(id, name, public, file_size_limit, allowed_mime_types)
values ('listing-photos', 'listing-photos', true, 5242880, array['image/jpeg']);
create policy "Owners upload listing photos" on storage.objects for insert to authenticated
with check (bucket_id = 'listing-photos' and (storage.foldername(name))[1] = (select auth.uid())::text);
create policy "Owners remove listing photos" on storage.objects for delete to authenticated
using (bucket_id = 'listing-photos' and (storage.foldername(name))[1] = (select auth.uid())::text);
commit;
