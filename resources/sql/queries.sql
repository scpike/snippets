-- :name get-all-snippets :? :*
-- retrieve all users.
SELECT * FROM snippets
order by created_at asc

-- :name get-snippet :? :*
-- get a snippet by id
SELECT * FROM snippets where id = :id
