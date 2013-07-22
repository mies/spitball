Why
====
Enable computers to understand not just the raw values of the metrics but to understand the meaning they are trying to communicate

How
===
Augment the l2met format to encode more semantic value into metrics


What
====


Current state
----------------------------
Currently the format of metrics is as follows

```
l2met trigger        val  unit
------------              ----  -
|            |             |     | | |
measure.name="123u"
              |         |
              ---------
              identifier 
```
