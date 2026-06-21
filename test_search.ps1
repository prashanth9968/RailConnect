try {
    $response = Invoke-WebRequest -Uri 'http://localhost:8080/api/v1/trains/search?fromStation=NDLS&toStation=BCT&journeyDate=2026-06-19' -UseBasicParsing
    $json = $response.Content | ConvertFrom-Json
    $train = $json.data[0]
    Write-Host "availableClasses:" ($train.availableClasses -join ", ")
    Write-Host "classAvailability keys:" ($train.classAvailability.PSObject.Properties.Name -join ", ")
    $train.classAvailability | ConvertTo-Json
} catch {
    Write-Host "ERROR: $_"
}
