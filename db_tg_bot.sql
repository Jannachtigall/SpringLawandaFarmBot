CREATE TABLE IF NOT EXISTS public.days
(
    day date NOT NULL,
    busy_times integer,
    CONSTRAINT days_pkey PRIMARY KEY (day)
);

CREATE TABLE IF NOT EXISTS public.order_products
(
    product_name character varying(20) COLLATE pg_catalog."default" NOT NULL,
    order_number character varying(20) COLLATE pg_catalog."default",
    count integer
);

CREATE TABLE IF NOT EXISTS public.orders
(
    user_name character varying(20) COLLATE pg_catalog."default",
    summa double precision,
    "number" character varying(20) COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT orders_pkey PRIMARY KEY ("number")
);

CREATE TABLE IF NOT EXISTS public.products
(
    "Name" character varying(20) COLLATE pg_catalog."default" NOT NULL,
    "Description" character varying(100) COLLATE pg_catalog."default" NOT NULL,
    "Price" double precision NOT NULL,
    "Photo" character varying(30) COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT "Products_pk" PRIMARY KEY ("Name")
);

CREATE TABLE IF NOT EXISTS public.times
(
    moment time without time zone NOT NULL,
    day date,
    program character varying(40) COLLATE pg_catalog."default",
    people integer
);

CREATE TABLE IF NOT EXISTS public.trash
(
    user_name character varying(20) COLLATE pg_catalog."default",
    product_name character varying(20) COLLATE pg_catalog."default",
    price double precision,
    count integer
);

CREATE TABLE IF NOT EXISTS public.users
(
    "UserName" character varying(20) COLLATE pg_catalog."default" NOT NULL,
    "FullName" character varying(100) COLLATE pg_catalog."default",
    "PhoneNumber" character varying(13) COLLATE pg_catalog."default",
    "Address" character varying(100) COLLATE pg_catalog."default",
    "ChatId" bigint,
    currentstate character varying(20) COLLATE pg_catalog."default",
    CONSTRAINT "Users_pk" PRIMARY KEY ("UserName")
);

CREATE TABLE IF NOT EXISTS public.visits
(
    user_name character varying(20) COLLATE pg_catalog."default",
    day date,
    moment time without time zone,
    "number" character varying(20) COLLATE pg_catalog."default",
    program character varying(40) COLLATE pg_catalog."default"
);

ALTER TABLE IF EXISTS public.order_products
    ADD CONSTRAINT "OrderProducts_fk1" FOREIGN KEY (product_name)
    REFERENCES public.products ("Name") MATCH SIMPLE
    ON UPDATE NO ACTION
    ON DELETE NO ACTION;


ALTER TABLE IF EXISTS public.order_products
    ADD CONSTRAINT order_products_order_number_fkey FOREIGN KEY (order_number)
    REFERENCES public.orders ("number") MATCH SIMPLE
    ON UPDATE NO ACTION
    ON DELETE NO ACTION;


ALTER TABLE IF EXISTS public.orders
    ADD CONSTRAINT orders_user_name_fkey FOREIGN KEY (user_name)
    REFERENCES public.users ("UserName") MATCH SIMPLE
    ON UPDATE NO ACTION
    ON DELETE NO ACTION;


ALTER TABLE IF EXISTS public.times
    ADD CONSTRAINT times_day_fkey FOREIGN KEY (day)
    REFERENCES public.days (day) MATCH SIMPLE
    ON UPDATE NO ACTION
    ON DELETE NO ACTION;


ALTER TABLE IF EXISTS public.trash
    ADD CONSTRAINT for_key_1 FOREIGN KEY (user_name)
    REFERENCES public.users ("UserName") MATCH SIMPLE
    ON UPDATE NO ACTION
    ON DELETE NO ACTION;


ALTER TABLE IF EXISTS public.trash
    ADD CONSTRAINT for_key_2 FOREIGN KEY (product_name)
    REFERENCES public.products ("Name") MATCH SIMPLE
    ON UPDATE NO ACTION
    ON DELETE NO ACTION;


ALTER TABLE IF EXISTS public.visits
    ADD CONSTRAINT visits_user_name_fkey FOREIGN KEY (user_name)
    REFERENCES public.users ("UserName") MATCH SIMPLE
    ON UPDATE NO ACTION
    ON DELETE NO ACTION;

END;