create table cards (
    id varchar(100) primary key,
    issuer_name varchar(100) not null,
    product_name varchar(200) not null,
    card_type varchar(20) not null,
    priority integer not null
);

create table card_performance_policies (
    card_id varchar(100) primary key,
    tiers_json text not null,
    benefit_rules_json text not null,
    constraint fk_card_performance_policies_card
        foreign key (card_id) references cards (id)
);

create table spending_records (
    id uuid primary key,
    card_id varchar(100) not null,
    amount bigint not null,
    spent_on date not null,
    merchant_name varchar(200) not null,
    merchant_category varchar(100) not null,
    payment_tags text not null,
    constraint fk_spending_records_card
        foreign key (card_id) references cards (id)
);

create index ix_spending_records_spent_on on spending_records (spent_on);
create index ix_spending_records_card_id on spending_records (card_id);
