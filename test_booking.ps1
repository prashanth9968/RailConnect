
$ts = [DateTimeOffset]::Now.ToUnixTimeSeconds()
$phone = "80000$($ts.ToString().Substring(5))"
$regBody = "{`"firstName`":`"Test`",`"lastName`":`"User`",`"email`":`"tu$ts@test.in`",`"password`":`"Test@1234`",`"phone`":`"$phone`"}"

try {
    $regRes = Invoke-WebRequest -Uri 'http://localhost:8080/api/v1/auth/register' -Method POST -Body $regBody -ContentType "application/json" -UseBasicParsing
    $token = ($regRes.Content | ConvertFrom-Json).data.accessToken
    Write-Host "Registered OK"
} catch {
    $stream = $_.Exception.Response.GetResponseStream(); $reader = New-Object System.IO.StreamReader($stream)
    Write-Host "FAIL: $($reader.ReadToEnd())"; exit 1
}

$h = @{ "Content-Type"="application/json"; "Authorization"="Bearer $token" }

# Test 1: with field name "age"  
Write-Host "--- Test: age (not passengerAge) ---"
$b1 = '{"trainId":31,"journeyDate":"2026-06-21","seatClass":"S3","sourceStationCode":"NDLS","destinationStationCode":"BCT","passengers":[{"passengerName":"Test User","age":30,"passengerGender":"MALE","berthPreference":"NO_PREFERENCE"}]}'
try {
    $r = Invoke-WebRequest -Uri 'http://localhost:8080/api/v1/bookings' -Method POST -Body $b1 -Headers $h -UseBasicParsing
    Write-Host "OK: $(($r.Content | ConvertFrom-Json).data.pnrNumber)"
} catch {
    $stream = $_.Exception.Response.GetResponseStream(); $reader = New-Object System.IO.StreamReader($stream)
    Write-Host "FAIL: $($reader.ReadToEnd())"
}

# Test 2: with field name "passengerAge" + integer
Write-Host "--- Test: passengerAge as integer ---"
$b2 = '{"trainId":31,"journeyDate":"2026-06-21","seatClass":"S3","sourceStationCode":"NDLS","destinationStationCode":"BCT","passengers":[{"passengerName":"Test User","passengerAge":30,"passengerGender":"MALE","berthPreference":"NO_PREFERENCE"}]}'
try {
    $r = Invoke-WebRequest -Uri 'http://localhost:8080/api/v1/bookings' -Method POST -Body $b2 -Headers $h -UseBasicParsing
    Write-Host "OK: $(($r.Content | ConvertFrom-Json).data.pnrNumber)"
} catch {
    $stream = $_.Exception.Response.GetResponseStream(); $reader = New-Object System.IO.StreamReader($stream)
    Write-Host "FAIL: $($reader.ReadToEnd())"
}

# Test 3: with "gender" (not passengerGender) 
Write-Host "--- Test: gender shorthand + age ---"
$b3 = '{"trainId":31,"journeyDate":"2026-06-21","seatClass":"S3","sourceStationCode":"NDLS","destinationStationCode":"BCT","passengers":[{"name":"Test User","age":30,"gender":"MALE","berthPreference":"NO_PREFERENCE"}]}'
try {
    $r = Invoke-WebRequest -Uri 'http://localhost:8080/api/v1/bookings' -Method POST -Body $b3 -Headers $h -UseBasicParsing
    Write-Host "OK: $(($r.Content | ConvertFrom-Json).data.pnrNumber)"
} catch {
    $stream = $_.Exception.Response.GetResponseStream(); $reader = New-Object System.IO.StreamReader($stream)
    Write-Host "FAIL: $($reader.ReadToEnd())"
}
