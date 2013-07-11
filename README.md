Spitball
========

Build metric aggregation as a (very simple) service.

How It Works
------------
1. GitProxy and Codon drain their logs to Spitball.
2. Spitball parses out the metrics and sends them into a volatile Redis hash grouped by `request_id`.
3. Vacuole pulls the aggregated metrics from Spitball and stores them in its database.