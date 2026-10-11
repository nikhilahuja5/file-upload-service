param(
    [string]$BaseUrl = "http://localhost:8080"
)

$scriptBlock = {
    param($BaseUrl, $UserId, $FileNamePrefix, $Count)

    $results = @()
    for ($i = 1; $i -le $Count; $i++) {
        $headers = @{ "X-User-Id" = $UserId }
        $body = @{ fileName = "$FileNamePrefix-$i.txt"; mimeType = "text/plain"; totalSizeBytes = 100 } | ConvertTo-Json

        try {
            $response = Invoke-RestMethod -Uri "$BaseUrl/api/files" -Method Post -Headers $headers -ContentType "application/json" -Body $body
            $results += "[$UserId] Request $i -> 201 Created, fileId: $($response.id)"
        } catch {
            $statusCode = $_.Exception.Response.StatusCode.value__
            $stream = $_.Exception.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($stream)
            $errorBody = $reader.ReadToEnd()
            $results += "[$UserId] Request $i -> $statusCode : $errorBody"
        }
    }
    return $results
}

Write-Output "Starting concurrent rate-limit test against $BaseUrl ..."
Write-Output "User 1 (rate-limit-test-user) will send 11 requests, User 2 (second-test-user) will send 4 requests, at the same time.`n"

$job1 = Start-Job -ScriptBlock $scriptBlock -ArgumentList $BaseUrl, "rate-limit-test-user", "concurrent-test-user1", 11
$job2 = Start-Job -ScriptBlock $scriptBlock -ArgumentList $BaseUrl, "second-test-user", "concurrent-test-user2", 4

Wait-Job $job1, $job2 | Out-Null

Write-Output "--- User 1 (rate-limit-test-user) results ---"
Receive-Job $job1

Write-Output "`n--- User 2 (second-test-user) results ---"
Receive-Job $job2

Remove-Job $job1, $job2

Write-Output "`nExpected: User 1 gets 201 x10 then 429 on the 11th. User 2 gets 201 x4 with no 429s at all, regardless of what User 1 is doing."
