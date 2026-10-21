[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string]$Issuer,

    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string]$ClientId,

    [ValidateRange(15, 600)]
    [int]$TimeoutSeconds = 180,

    [switch]$ShowAccessTokenClaims,

    [switch]$ProbeCustomerService
)

Set-StrictMode -Version 2.0
$ErrorActionPreference = 'Stop'

$redirectUri = 'http://127.0.0.1:8765/callback'
$callbackPort = 8765
$listener = $null
$listenerStarted = $false
$client = $null
$stream = $null
$reader = $null
$codeVerifier = $null
$state = $null
$nonce = $null
$authorizationCode = $null
$tokenResponse = $null
$accessToken = $null
$accessClaims = $null
$tamperedAccessToken = $null
$idToken = $null
$idClaims = $null
$exitCode = 0
$customerServiceProbeUri = 'http://localhost:8082/test/security/principal'

function Stop-WithSafeError {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Message
    )

    $exception = [System.InvalidOperationException]::new($Message)
    $exception.Data['OidcSafeMessage'] = $Message
    throw $exception
}

function ConvertTo-Base64Url {
    param(
        [Parameter(Mandatory = $true)]
        [byte[]]$Bytes
    )

    $encoded = [System.Convert]::ToBase64String($Bytes)
    return $encoded.TrimEnd([char[]]'=').Replace('+', '-').Replace('/', '_')
}

function New-CryptographicValue {
    param(
        [Parameter(Mandatory = $true)]
        [ValidateRange(32, 64)]
        [int]$ByteCount
    )

    $bytes = [byte[]]::new($ByteCount)
    $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()

    try {
        $generator.GetBytes($bytes)
        return ConvertTo-Base64Url -Bytes $bytes
    }
    finally {
        [System.Array]::Clear($bytes, 0, $bytes.Length)
        $generator.Dispose()
    }
}

function Test-FixedTimeStringEquals {
    param(
        [string]$Expected,
        [string]$Actual
    )

    if ($null -eq $Expected -or $null -eq $Actual) {
        return $false
    }

    $expectedBytes = [System.Text.Encoding]::ASCII.GetBytes($Expected)
    $actualBytes = [System.Text.Encoding]::ASCII.GetBytes($Actual)

    try {
        if ($expectedBytes.Length -ne $actualBytes.Length) {
            return $false
        }

        $difference = 0
        for ($index = 0; $index -lt $expectedBytes.Length; $index++) {
            $difference = $difference -bor ($expectedBytes[$index] -bxor $actualBytes[$index])
        }

        return ($difference -eq 0)
    }
    finally {
        [System.Array]::Clear($expectedBytes, 0, $expectedBytes.Length)
        [System.Array]::Clear($actualBytes, 0, $actualBytes.Length)
    }
}

function Send-CallbackResponse {
    param(
        [Parameter(Mandatory = $true)]
        [System.IO.Stream]$ResponseStream,

        [Parameter(Mandatory = $true)]
        [int]$StatusCode,

        [Parameter(Mandatory = $true)]
        [string]$ReasonPhrase,

        [Parameter(Mandatory = $true)]
        [string]$Message
    )

    $html = '<!doctype html><html><head><meta charset="utf-8"><title>OIDC local test</title></head><body><p>' +
        $Message +
        '</p><p>You may return to the PowerShell window.</p><script>window.history.replaceState(null, document.title, ''/callback'');</script></body></html>'
    $bodyBytes = [System.Text.Encoding]::UTF8.GetBytes($html)
    $headers = "HTTP/1.1 $StatusCode $ReasonPhrase`r`n" +
        "Content-Type: text/html; charset=utf-8`r`n" +
        "Cache-Control: no-store`r`n" +
        "Referrer-Policy: no-referrer`r`n" +
        "X-Content-Type-Options: nosniff`r`n" +
        "Content-Security-Policy: default-src 'none'; script-src 'unsafe-inline'; base-uri 'none'; frame-ancestors 'none'`r`n" +
        "Content-Length: $($bodyBytes.Length)`r`n" +
        "Connection: close`r`n`r`n"
    $headerBytes = [System.Text.Encoding]::ASCII.GetBytes($headers)

    $ResponseStream.Write($headerBytes, 0, $headerBytes.Length)
    $ResponseStream.Write($bodyBytes, 0, $bodyBytes.Length)
    $ResponseStream.Flush()
}

function ConvertFrom-UnverifiedJwtPayload {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Jwt
    )

    $parts = $Jwt.Split('.')
    if ($parts.Length -ne 3 -or [string]::IsNullOrEmpty($parts[1])) {
        Stop-WithSafeError 'Keycloak did not return a JWT in the expected format.'
    }

    $payload = $parts[1].Replace('-', '+').Replace('_', '/')
    switch ($payload.Length % 4) {
        0 { }
        2 { $payload += '==' }
        3 { $payload += '=' }
        default { Stop-WithSafeError 'The JWT payload could not be decoded.' }
    }

    $payloadBytes = $null
    try {
        $payloadBytes = [System.Convert]::FromBase64String($payload)
        $payloadJson = [System.Text.Encoding]::UTF8.GetString($payloadBytes)
        return (ConvertFrom-Json -InputObject $payloadJson -ErrorAction Stop)
    }
    catch {
        Stop-WithSafeError 'The JWT payload could not be decoded.'
    }
    finally {
        if ($null -ne $payloadBytes) {
            [System.Array]::Clear($payloadBytes, 0, $payloadBytes.Length)
        }
    }
}

function Get-SafeClaimText {
    param(
        [AllowNull()]
        [object]$Value,

        [Parameter(Mandatory = $true)]
        [int]$MaximumLength
    )

    if ($Value -isnot [string] -or
        $Value.Length -eq 0 -or
        $Value.Length -gt $MaximumLength -or
        $Value -match '[\x00-\x1F\x7F]') {
        return '(absent or invalid)'
    }

    return $Value
}

function Write-AccessTokenClaimSummary {
    param(
        [Parameter(Mandatory = $true)]
        [psobject]$Claims
    )

    $issuerValue = $null
    $issuerProperty = $Claims.PSObject.Properties['iss']
    if ($null -ne $issuerProperty) {
        $issuerValue = $issuerProperty.Value
    }
    $issuer = Get-SafeClaimText -Value $issuerValue -MaximumLength 512

    $subjectValue = $null
    $subjectProperty = $Claims.PSObject.Properties['sub']
    if ($null -ne $subjectProperty) {
        $subjectValue = $subjectProperty.Value
    }
    $subject = Get-SafeClaimText -Value $subjectValue -MaximumLength 255
    $audiences = @()
    $audienceProperty = $Claims.PSObject.Properties['aud']
    if ($null -ne $audienceProperty) {
        foreach ($audience in @($audienceProperty.Value)) {
            $safeAudience = Get-SafeClaimText -Value $audience -MaximumLength 256
            if ($safeAudience -cne '(absent or invalid)') {
                $audiences += $safeAudience
            }
        }
    }

    $expiration = 'absent or invalid'
    $expirationProperty = $Claims.PSObject.Properties['exp']
    if ($null -ne $expirationProperty) {
        try {
            $expiration = [DateTimeOffset]::FromUnixTimeSeconds(
                [long]$expirationProperty.Value
            ).UtcDateTime.ToString('o')
        }
        catch {
            $expiration = 'absent or invalid'
        }
    }

    $roles = @()
    $realmAccessProperty = $Claims.PSObject.Properties['realm_access']
    if ($null -ne $realmAccessProperty -and $null -ne $realmAccessProperty.Value) {
        $rolesProperty = $realmAccessProperty.Value.PSObject.Properties['roles']
        if ($null -ne $rolesProperty -and $null -ne $rolesProperty.Value) {
            foreach ($role in @($rolesProperty.Value)) {
                if ($role -ceq 'CUSTOMER' -or $role -ceq 'OPERATOR') {
                    $roles += [string]$role
                }
            }
        }
    }

    Write-Host 'Access-token claims below are decoded locally; this script does not verify their signature.'
    Write-Host ("access-token iss: {0}" -f $issuer)
    Write-Host ("access-token sub: {0}" -f $subject)
    Write-Host ("access-token aud: {0}" -f $(if ($audiences.Count -gt 0) {
                $audiences -join ', '
            } else {
                '(absent)'
            }))
    Write-Host ("access-token exp (UTC): {0}" -f $expiration)
    Write-Host ("access-token realm roles: {0}" -f $(if ($roles.Count -gt 0) {
                $roles -join ', '
            } else {
                '(CUSTOMER/OPERATOR absent)'
            }))
}

function Invoke-CustomerServiceSecurityProbe {
    param(
        [AllowNull()]
        [string]$BearerToken
    )

    $request = [System.Net.HttpWebRequest]::Create($customerServiceProbeUri)
    $request.Method = 'GET'
    $request.Timeout = 15000
    $request.ReadWriteTimeout = 15000
    $request.AllowAutoRedirect = $false
    $request.Proxy = $null
    if (-not [string]::IsNullOrEmpty($BearerToken)) {
        $request.Headers[[System.Net.HttpRequestHeader]::Authorization] = "Bearer $BearerToken"
    }

    $response = $null
    $body = ''
    try {
        $response = [System.Net.HttpWebResponse]$request.GetResponse()
    }
    catch [System.Net.WebException] {
        if ($null -eq $_.Exception.Response) {
            Stop-WithSafeError 'Could not reach customer-service at http://localhost:8082.'
        }
        $response = [System.Net.HttpWebResponse]$_.Exception.Response
    }

    try {
        if ([int]$response.StatusCode -eq 200) {
            $reader = [System.IO.StreamReader]::new($response.GetResponseStream())
            try {
                $body = $reader.ReadToEnd()
            }
            finally {
                $reader.Dispose()
            }
        }
        return [pscustomobject]@{
            StatusCode = [int]$response.StatusCode
            Body       = $body
        }
    }
    finally {
        $response.Close()
    }
}

function New-TamperedJwtSignature {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Jwt
    )

    $parts = $Jwt.Split('.')
    if ($parts.Length -ne 3 -or [string]::IsNullOrEmpty($parts[2])) {
        Stop-WithSafeError 'The access token cannot be used for the signature-tampering check.'
    }

    # Modifier un caractère au milieu de la signature change effectivement les octets signés.
    $signature = [string]$parts[2]
    $index = [int][Math]::Floor($signature.Length / 2)
    $replacement = if ($signature[$index] -ceq 'A') { 'B' } else { 'A' }
    $parts[2] = $signature.Substring(0, $index) + $replacement + $signature.Substring($index + 1)
    return ($parts -join '.')
}

try {
    try {
        $issuerUri = [System.Uri]$Issuer
    }
    catch {
        Stop-WithSafeError 'Issuer must be an absolute local Keycloak URL.'
    }

    if (-not $issuerUri.IsAbsoluteUri -or
        $issuerUri.Scheme -cne 'http' -or
        -not $issuerUri.IsLoopback -or
        $issuerUri.Port -ne 8081 -or
        -not [string]::IsNullOrEmpty($issuerUri.UserInfo) -or
        -not [string]::IsNullOrEmpty($issuerUri.Query) -or
        -not [string]::IsNullOrEmpty($issuerUri.Fragment)) {
        Stop-WithSafeError 'Issuer must use the local Keycloak HTTP endpoint on port 8081.'
    }

    $expectedIssuer = $issuerUri.AbsoluteUri.TrimEnd('/')
    $discoveryUri = $expectedIssuer + '/.well-known/openid-configuration'

    try {
        Add-Type -AssemblyName System.Web
        $metadata = Invoke-RestMethod `
            -Uri $discoveryUri `
            -Method Get `
            -TimeoutSec 20 `
            -UseBasicParsing `
            -ErrorAction Stop
    }
    catch {
        Stop-WithSafeError 'OIDC discovery failed. Confirm that Keycloak is running and the issuer URL is correct.'
    }

    if ([string]$metadata.issuer -cne $expectedIssuer) {
        Stop-WithSafeError 'The issuer in the discovery document does not match the requested issuer.'
    }
    if ([string]::IsNullOrEmpty([string]$metadata.authorization_endpoint) -or
        [string]::IsNullOrEmpty([string]$metadata.token_endpoint)) {
        Stop-WithSafeError 'The discovery document is missing a required OIDC endpoint.'
    }

    foreach ($endpointValue in @($metadata.authorization_endpoint, $metadata.token_endpoint)) {
        try {
            $endpointUri = [System.Uri]$endpointValue
        }
        catch {
            Stop-WithSafeError 'The discovery document contains an invalid OIDC endpoint.'
        }

        if (-not $endpointUri.IsAbsoluteUri -or
            $endpointUri.Scheme -cne 'http' -or
            -not $endpointUri.IsLoopback -or
            $endpointUri.Port -ne $issuerUri.Port -or
            -not [string]::IsNullOrEmpty($endpointUri.UserInfo)) {
            Stop-WithSafeError 'OIDC endpoints must remain on the local Keycloak host.'
        }
    }

    $authorizationEndpoint = [System.Uri]$metadata.authorization_endpoint
    $tokenEndpoint = [System.Uri]$metadata.token_endpoint
    $codeVerifier = New-CryptographicValue -ByteCount 32
    $state = New-CryptographicValue -ByteCount 32
    $nonce = New-CryptographicValue -ByteCount 32

    $sha256 = [System.Security.Cryptography.SHA256]::Create()
    $verifierBytes = [System.Text.Encoding]::ASCII.GetBytes($codeVerifier)
    $challengeBytes = $null
    try {
        $challengeBytes = $sha256.ComputeHash($verifierBytes)
        $codeChallenge = ConvertTo-Base64Url -Bytes $challengeBytes
    }
    finally {
        [System.Array]::Clear($verifierBytes, 0, $verifierBytes.Length)
        if ($null -ne $challengeBytes) {
            [System.Array]::Clear($challengeBytes, 0, $challengeBytes.Length)
        }
        $sha256.Dispose()
    }

    $authorizationParameters = [ordered]@{
        client_id             = $ClientId
        redirect_uri          = $redirectUri
        response_type         = 'code'
        scope                 = 'openid profile email'
        state                 = $state
        nonce                 = $nonce
        code_challenge        = $codeChallenge
        code_challenge_method = 'S256'
    }
    $encodedParameters = @()
    foreach ($key in $authorizationParameters.Keys) {
        $encodedKey = [System.Uri]::EscapeDataString([string]$key)
        $encodedValue = [System.Uri]::EscapeDataString([string]$authorizationParameters[$key])
        $encodedParameters += ($encodedKey + '=' + $encodedValue)
    }
    $authorizationUriBuilder = [System.UriBuilder]::new($authorizationEndpoint)
    $authorizationUriBuilder.Query = [string]::Join('&', $encodedParameters)
    $authorizationUri = $authorizationUriBuilder.Uri

    $listener = [System.Net.Sockets.TcpListener]::new(
        [System.Net.IPAddress]::Loopback,
        $callbackPort
    )

    try {
        $listener.Server.ExclusiveAddressUse = $true
        $listener.Start()
        $listenerStarted = $true
    }
    catch {
        Stop-WithSafeError 'Could not listen on 127.0.0.1:8765. The callback port may already be occupied.'
    }

    try {
        try {
            Start-Process -FilePath $authorizationUri.AbsoluteUri -ErrorAction Stop | Out-Null
        }
        catch {
            Stop-WithSafeError 'Could not open the browser. Open the local Keycloak login page manually and retry the script.'
        }

        $deadline = [System.DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
        while (-not $listener.Pending()) {
            if ([System.DateTime]::UtcNow -ge $deadline) {
                Stop-WithSafeError "No callback arrived within $TimeoutSeconds seconds. The listener was closed."
            }
            Start-Sleep -Milliseconds 100
        }

        $client = $listener.AcceptTcpClient()
        $client.ReceiveTimeout = 10000
        $stream = $client.GetStream()
        $reader = [System.IO.StreamReader]::new($stream, [System.Text.Encoding]::ASCII)
        $requestLine = $reader.ReadLine()

        if ([string]::IsNullOrEmpty($requestLine) -or
            $requestLine.Length -gt 8192 -or
            $requestLine -cnotmatch '^GET\s+(\S+)\s+HTTP/1\.[01]$') {
            Send-CallbackResponse -ResponseStream $stream -StatusCode 400 -ReasonPhrase 'Bad Request' -Message 'The local callback could not be accepted.'
            Stop-WithSafeError 'The local callback request was invalid.'
        }
        $requestTarget = $Matches[1]

        $headersComplete = $false
        for ($headerIndex = 0; $headerIndex -lt 64; $headerIndex++) {
            $headerLine = $reader.ReadLine()
            if ($null -eq $headerLine) {
                break
            }
            if ($headerLine.Length -gt 8192) {
                break
            }
            if ($headerLine.Length -eq 0) {
                $headersComplete = $true
                break
            }
        }
        if (-not $headersComplete) {
            Send-CallbackResponse -ResponseStream $stream -StatusCode 400 -ReasonPhrase 'Bad Request' -Message 'The local callback could not be accepted.'
            Stop-WithSafeError 'The local callback request was incomplete.'
        }

        try {
            $callbackUri = [System.Uri]('http://127.0.0.1:8765' + $requestTarget)
        }
        catch {
            Send-CallbackResponse -ResponseStream $stream -StatusCode 400 -ReasonPhrase 'Bad Request' -Message 'The local callback could not be accepted.'
            Stop-WithSafeError 'The local callback URL was invalid.'
        }

        if ($callbackUri.Host -cne '127.0.0.1' -or
            $callbackUri.Port -ne $callbackPort -or
            $callbackUri.AbsolutePath -cne '/callback') {
            Send-CallbackResponse -ResponseStream $stream -StatusCode 404 -ReasonPhrase 'Not Found' -Message 'The local callback path was not accepted.'
            Stop-WithSafeError 'The callback did not use the expected local path.'
        }

        try {
            $query = [System.Web.HttpUtility]::ParseQueryString($callbackUri.Query)
        }
        catch {
            Send-CallbackResponse -ResponseStream $stream -StatusCode 400 -ReasonPhrase 'Bad Request' -Message 'The local callback could not be accepted.'
            Stop-WithSafeError 'The local callback query could not be parsed.'
        }

        $stateValues = @($query.GetValues('state'))
        if ($stateValues.Count -ne 1 -or
            [string]::IsNullOrEmpty([string]$stateValues[0]) -or
            -not (Test-FixedTimeStringEquals -Expected $state -Actual ([string]$stateValues[0]))) {
            Send-CallbackResponse -ResponseStream $stream -StatusCode 400 -ReasonPhrase 'Bad Request' -Message 'The authorization response state did not match.'
            Stop-WithSafeError 'The authorization response state did not match; the code was rejected.'
        }

        $responseIssuer = [string]$query['iss']
        if (-not [string]::IsNullOrEmpty($responseIssuer) -and $responseIssuer -cne $expectedIssuer) {
            Send-CallbackResponse -ResponseStream $stream -StatusCode 400 -ReasonPhrase 'Bad Request' -Message 'The authorization response issuer did not match.'
            Stop-WithSafeError 'The authorization response issuer did not match.'
        }

        $oidcError = [string]$query['error']
        if (-not [string]::IsNullOrEmpty($oidcError)) {
            $safeErrorCode = 'authorization_error'
            if (@('access_denied', 'invalid_request', 'invalid_redirect_uri', 'invalid_grant', 'unauthorized_client', 'login_required', 'interaction_required') -ccontains $oidcError) {
                $safeErrorCode = $oidcError
            }
            Send-CallbackResponse -ResponseStream $stream -StatusCode 400 -ReasonPhrase 'Bad Request' -Message 'Keycloak returned an authorization error. Return to PowerShell.'
            Stop-WithSafeError "Keycloak returned an OIDC authorization error ($safeErrorCode)."
        }

        $codeValues = @($query.GetValues('code'))
        if ($codeValues.Count -ne 1 -or [string]::IsNullOrEmpty([string]$codeValues[0])) {
            Send-CallbackResponse -ResponseStream $stream -StatusCode 400 -ReasonPhrase 'Bad Request' -Message 'The authorization response did not contain a usable code.'
            Stop-WithSafeError 'The authorization response did not contain a usable code.'
        }

        $authorizationCode = [string]$codeValues[0]
        Send-CallbackResponse -ResponseStream $stream -StatusCode 200 -ReasonPhrase 'OK' -Message 'Authorization response received.'
    }
    finally {
        if ($null -ne $reader) {
            $reader.Dispose()
            $reader = $null
        }
        if ($null -ne $client) {
            $client.Close()
            $client = $null
        }
        if ($listenerStarted) {
            $listener.Stop()
            $listenerStarted = $false
        }
    }

    $tokenForm = @{
        grant_type    = 'authorization_code'
        client_id     = $ClientId
        code          = $authorizationCode
        redirect_uri  = $redirectUri
        code_verifier = $codeVerifier
    }

    try {
        $tokenResponse = Invoke-RestMethod `
            -Uri $tokenEndpoint `
            -Method Post `
            -ContentType 'application/x-www-form-urlencoded' `
            -Body $tokenForm `
            -TimeoutSec 20 `
            -UseBasicParsing `
            -ErrorAction Stop
    }
    catch {
        Stop-WithSafeError 'The token exchange failed. Check the client, exact redirect URI, PKCE S256 setting, and whether the authorization code expired.'
    }
    finally {
        $authorizationCode = $null
        $codeVerifier = $null
    }

    $idToken = [string]$tokenResponse.id_token
    if ([string]::IsNullOrEmpty($idToken)) {
        Stop-WithSafeError 'Keycloak did not return an ID token. No token value was displayed.'
    }
    $accessToken = [string]$tokenResponse.access_token
    if ([string]::IsNullOrEmpty($accessToken)) {
        Stop-WithSafeError 'Keycloak did not return an access token. No token value was displayed.'
    }

    $idClaims = ConvertFrom-UnverifiedJwtPayload -Jwt $idToken
    if ([string]$idClaims.iss -cne $expectedIssuer) {
        Stop-WithSafeError 'The ID token issuer does not match the expected issuer.'
    }
    if (-not (Test-FixedTimeStringEquals -Expected $nonce -Actual ([string]$idClaims.nonce))) {
        Stop-WithSafeError 'The ID token nonce did not match the value from the authorization request.'
    }

    $audiences = @($idClaims.aud)
    if ($audiences.Count -eq 0 -or $audiences -cnotcontains $ClientId) {
        Stop-WithSafeError 'The ID token audience does not include the requested client.'
    }
    if ($audiences.Count -gt 1 -and [string]$idClaims.azp -cne $ClientId) {
        Stop-WithSafeError 'The ID token authorized party does not match the requested client.'
    }

    $subject = [string]$idClaims.sub
    if ([string]::IsNullOrWhiteSpace($subject) -or
        $subject.Length -gt 255 -or
        $subject -match '[\x00-\x1F\x7F]') {
        Stop-WithSafeError 'The ID token did not contain a subject.'
    }

    $emailVerified = 'not present'
    $emailVerifiedProperty = $idClaims.PSObject.Properties['email_verified']
    if ($null -ne $emailVerifiedProperty -and $emailVerifiedProperty.Value -is [bool]) {
        $emailVerified = [string]$emailVerifiedProperty.Value
    }

    $allowedRoles = @()
    $realmAccessProperty = $idClaims.PSObject.Properties['realm_access']
    if ($null -ne $realmAccessProperty -and $null -ne $realmAccessProperty.Value) {
        $realmRolesProperty = $realmAccessProperty.Value.PSObject.Properties['roles']
        if ($null -ne $realmRolesProperty -and $null -ne $realmRolesProperty.Value) {
            foreach ($role in @($realmRolesProperty.Value)) {
                if ($role -ceq 'CUSTOMER' -or $role -ceq 'OPERATOR') {
                    $allowedRoles += [string]$role
                }
            }
        }
    }

    Write-Host 'OIDC authorization completed. The ID token signature was not verified by this script.'
    Write-Host ("iss: {0}" -f [string]$idClaims.iss)
    Write-Host ("sub: {0}" -f $subject)
    Write-Host ("email_verified: {0}" -f $emailVerified)
    if ($allowedRoles.Count -gt 0) {
        Write-Host ("realm roles: {0}" -f ($allowedRoles -join ', '))
    }
    else {
        Write-Host 'realm roles: (none of CUSTOMER or OPERATOR was present)'
    }

    if ($ShowAccessTokenClaims -or $ProbeCustomerService) {
        $accessClaims = ConvertFrom-UnverifiedJwtPayload -Jwt $accessToken
        Write-AccessTokenClaimSummary -Claims $accessClaims
    }

    if ($ProbeCustomerService) {
        # La route n'existe que dans src/test; les statuts et claims prouvent le passage du vrai filtre JWT.
        $withoutTokenResponse = Invoke-CustomerServiceSecurityProbe -BearerToken $null
        $validTokenResponse = Invoke-CustomerServiceSecurityProbe -BearerToken $accessToken
        $tamperedAccessToken = New-TamperedJwtSignature -Jwt $accessToken
        $tamperedTokenResponse = Invoke-CustomerServiceSecurityProbe -BearerToken $tamperedAccessToken

        Write-Host ("Customer Service probe without token: HTTP {0} (expected 401)" -f $withoutTokenResponse.StatusCode)
        Write-Host ("Customer Service probe with Keycloak token: HTTP {0} (expected 200)" -f $validTokenResponse.StatusCode)
        Write-Host ("Customer Service probe with altered signature: HTTP {0} (expected 401)" -f $tamperedTokenResponse.StatusCode)
        if ($validTokenResponse.StatusCode -eq 200) {
            try {
                $principalResponse = ConvertFrom-Json -InputObject $validTokenResponse.Body -ErrorAction Stop
                $verifiedSubject = Get-SafeClaimText -Value $principalResponse.sub -MaximumLength 255
                $verifiedRoles = @()
                foreach ($role in @($principalResponse.authorities)) {
                    if ($role -ceq 'CUSTOMER' -or $role -ceq 'OPERATOR') {
                        $verifiedRoles += [string]$role
                    }
                }
                Write-Host ("Verified principal sub: {0}" -f $verifiedSubject)
                Write-Host ("Verified principal roles: {0}" -f $(if ($verifiedRoles.Count -gt 0) {
                            $verifiedRoles -join ', '
                        } else {
                            '(CUSTOMER/OPERATOR absent)'
                        }))
            }
            catch {
                Stop-WithSafeError 'The test-only principal response was not in the expected safe JSON format.'
            }
        }
    }
}
catch {
    $safeMessage = 'OIDC flow failed. No token, authorization code, or verifier was displayed.'
    if ($_.Exception.Data.Contains('OidcSafeMessage')) {
        $safeMessage = [string]$_.Exception.Data['OidcSafeMessage']
    }
    Write-Error -Message $safeMessage -ErrorAction Continue
    $exitCode = 1
}
finally {
    if ($listenerStarted -and $null -ne $listener) {
        $listener.Stop()
        $listenerStarted = $false
    }
    if ($null -ne $reader) {
        $reader.Dispose()
    }
    if ($null -ne $client) {
        $client.Close()
    }

    $authorizationCode = $null
    $codeVerifier = $null
    $state = $null
    $nonce = $null
    $authorizationParameters = $null
    $encodedParameters = $null
    $authorizationUri = $null
    $tokenForm = $null
    $requestLine = $null
    $requestTarget = $null
    $callbackUri = $null
    $query = $null
    $stateValues = $null
    $codeValues = $null
    $oidcError = $null
    $responseIssuer = $null
    $Matches = @{}
    $tokenResponse = $null
    $accessToken = $null
    $accessClaims = $null
    $tamperedAccessToken = $null
    $idToken = $null
    $idClaims = $null
}

if ($exitCode -ne 0) {
    exit $exitCode
}
