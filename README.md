[![wercker status](https://app.wercker.com/status/cd189eb7defac15cc7e2dcba3366a1f8/m "wercker status")](https://app.wercker.com/project/bykey/cd189eb7defac15cc7e2dcba3366a1f8)


Spitball
========

Request-based metric collection as a (very simple) service.
Drain to spitball.herokuapp.com/v1/drain

How It Works
------------
1. GitProxy and Codon drain their logs to Spitball.
2. Spitball parses out the metrics and sends them into a volatile Redis hash grouped by `request_id`.
3. Vacuole pulls the aggregated metrics from Spitball and stores them in its database.
