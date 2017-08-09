SELECT
  data.time,
  data.van_id,
  vans.name,
  data.latitude,
  data.longitude
FROM data
  INNER JOIN vans
    ON data.van_id = vans.id
WHERE time > DATEADD('MINUTE', -?, NOW())