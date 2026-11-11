$base = "http://localhost:8081"

function Chamar($metodo, $caminho, $corpo = $null, $token = $null, $chave = $null) {
    $cabecalhos = @{ "Content-Type" = "application/json" }
    if ($token) { $cabecalhos["Authorization"] = "Bearer $token" }
    if ($chave) { $cabecalhos["Idempotency-Key"] = $chave }
    $parametros = @{ Method = $metodo; Uri = "$base$caminho"; Headers = $cabecalhos }
    if ($corpo) { $parametros["Body"] = ($corpo | ConvertTo-Json) }
    $resposta = Invoke-RestMethod @parametros
    $resposta
}

Write-Host "1) Logins do organizador e da portaria (carga inicial de laboratorio)"
$org = (Chamar POST "/auth/login" @{ email = "organizador@bilheteria.local"; senha = "organizador-123" }).accessToken
$portaria = (Chamar POST "/auth/login" @{ email = "portaria@bilheteria.local"; senha = "portaria-123" }).accessToken

Write-Host "2) Cadastro e login de um cliente"
$email = "cliente-$([guid]::NewGuid().ToString('N').Substring(0, 8))@exemplo.com"
Chamar POST "/usuarios" @{ nome = "Cliente Demo"; email = $email; senha = "senha-forte-1" } | Out-Null
$cliente = (Chamar POST "/auth/login" @{ email = $email; senha = "senha-forte-1" }).accessToken

Write-Host "3) Organizador cria, configura e publica o evento"
$agora = [DateTime]::UtcNow
$evento = Chamar POST "/eventos" @{ nome = "Show Demo"; local = "Arena"; dataDoEvento = $agora.AddDays(30).ToString("o"); capacidade = 100 } $org
Chamar POST "/eventos/$($evento.id)/categorias" @{ nome = "Pista"; precoEmCentavos = 5000; capacidade = 50; inicioDasVendas = $agora.AddMinutes(-1).ToString("o"); fimDasVendas = $agora.AddDays(29).ToString("o") } $org | Out-Null
Chamar POST "/eventos/$($evento.id)/publicacao" $null $org | Out-Null

Write-Host "4) Cliente reserva 2 ingressos (o dono vem do token, nao do corpo)"
$disponibilidade = @(Chamar GET "/disponibilidades?eventoId=$($evento.id)")[0]
$reserva = Chamar POST "/reservas" @{ categoriaId = $disponibilidade.categoriaId; quantidade = 2 } $cliente
"   reserva $($reserva.id): $($reserva.status), expira em $($reserva.expiraEm)"

Write-Host "5) Cliente paga (Idempotency-Key); a cadeia emite os ingressos na mesma transacao"
$chave = [guid]::NewGuid().ToString()
$pagamento = Chamar POST "/pagamentos" @{ reservaId = $reserva.id; tokenDePagamento = "tok-ok" } $cliente $chave
"   pagamento: $($pagamento.status)"
$repeticao = Chamar POST "/pagamentos" @{ reservaId = $reserva.id; tokenDePagamento = "tok-ok" } $cliente $chave
"   repeticao com a mesma chave devolve o mesmo pagamento: $($repeticao.id -eq $pagamento.id)"

Write-Host "6) Cliente lista os ingressos; a portaria faz o check-in (so vale uma vez)"
$ingressos = @(Chamar GET "/ingressos?reservaId=$($reserva.id)" $null $cliente)
"   $($ingressos.Count) ingressos emitidos"
$codigo = $ingressos[0].codigo
$checkIn = Chamar POST "/ingressos/$codigo/check-in" $null $portaria
"   check-in: $($checkIn.status)"
try { Chamar POST "/ingressos/$codigo/check-in" $null $portaria | Out-Null }
catch { "   segundo check-in recusado, como esperado (HTTP $([int]$_.Exception.Response.StatusCode))" }

Write-Host "7) Verificacao de consistencia do banco"
& "$PSScriptRoot/verificar-consistencia.ps1"