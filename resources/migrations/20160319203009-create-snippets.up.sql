CREATE OR REPLACE FUNCTION update_modified_column()
RETURNS TRIGGER AS $$
BEGIN
   IF row(NEW.*) IS DISTINCT FROM row(OLD.*) THEN
      NEW.updated_at = now();
      RETURN NEW;
   ELSE
      RETURN OLD;
   END IF;
END;
$$ language 'plpgsql';
--;;
CREATE TABLE snippets (
    id   SERIAL PRIMARY KEY,
    name varchar(255),
    slug varchar(255),
    code text,
    created_at timestamp default current_timestamp,
    updated_at timestamp
);
--;;
CREATE TRIGGER maintain_updated_at_on_snippets
    BEFORE UPDATE ON snippets
    FOR EACH ROW
    EXECUTE PROCEDURE update_modified_column();
