CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  UNIQUE NOT NULL,
    email         VARCHAR(120) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE ingredients (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    unit VARCHAR(20)  NOT NULL
);

CREATE TABLE recipes (
    id               BIGSERIAL PRIMARY KEY,
    title            VARCHAR(150) NOT NULL,
    description      TEXT,
    instructions     TEXT,
    servings         INT          NOT NULL DEFAULT 1,
    cooking_time_min INT,
    author_id        BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ
);
CREATE INDEX idx_recipes_author      ON recipes(author_id);
CREATE INDEX idx_recipes_title_lower ON recipes(lower(title));

CREATE TABLE recipe_ingredient (
    id            BIGSERIAL PRIMARY KEY,
    recipe_id     BIGINT NOT NULL REFERENCES recipes(id)     ON DELETE CASCADE,
    ingredient_id BIGINT NOT NULL REFERENCES ingredients(id) ON DELETE RESTRICT,
    quantity      NUMERIC(10,2) NOT NULL,
    unit          VARCHAR(20),
    note          VARCHAR(255),
    CONSTRAINT uq_recipe_ingredient UNIQUE (recipe_id, ingredient_id)
);
CREATE INDEX idx_recipe_ingredient_ingredient ON recipe_ingredient(ingredient_id);
