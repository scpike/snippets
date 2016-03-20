-- :name get-all-snippets :? :*
SELECT * FROM snippets
order by created_at asc

-- :name get-snippet :? :*
SELECT * FROM snippets where id = :id

-- :name insert-snippet :insert
INSERT INTO snippets (name, slug, code)
VALUES (:name, :slug, :code)

-- :name update-snippet :! :n
UPDATE snippets
  set name = :name,
      code = :code,
      slug = :slug
WHERE id = 10

-- :name delete-snippet :! :n
DELETE from snippets
  where id = :id
