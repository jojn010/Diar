-- Storage removal requires SELECT as well as DELETE. This only exposes an owner's own object metadata.
create policy "Owners read their photo metadata" on storage.objects for select to authenticated
using (bucket_id = 'listing-photos' and (storage.foldername(name))[1] = (select auth.uid())::text);
