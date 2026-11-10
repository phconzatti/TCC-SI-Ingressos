<#
 Confere os invariantes de negocio direto no banco.
 Codigos de saida: 0 = consistente, 1 = invariante violado, 2 = falha ao executar.
#>
param(
    [string]$Servico = "postgres",
    [string]$Banco   = "bilheteria",
    [string]$Usuario = "bilheteria"
)

Push-Location (Join-Path $PSScriptRoot "..")
try {
    $sql = Get-Content -Raw -Path (Join-Path $PSScriptRoot "consistencia.sql")
    $saida = $sql | docker compose exec -T $Servico psql -U $Usuario -d $Banco -v ON_ERROR_STOP=1 -At -F '|'
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Falha ao executar a verificacao no servico '$Servico'. O compose esta no ar?"
        exit 2
    }

    $linhas = $saida | Where-Object { $_ -match '\|' } | ForEach-Object {
        $c = $_ -split '\|', 4
        [pscustomobject]@{
            Ordem       = [int]$c[0]
            Gravidade   = $c[1]
            Verificacao = $c[2]
            Violacoes   = [long]$c[3]
        }
    }

    $linhas | Format-Table -AutoSize

    $erros  = @($linhas | Where-Object { $_.Gravidade -eq 'ERRO'  -and $_.Violacoes -gt 0 })
    $avisos = @($linhas | Where-Object { $_.Gravidade -eq 'AVISO' -and $_.Violacoes -gt 0 })

    if ($avisos.Count -gt 0) { Write-Warning "$($avisos.Count) aviso(s): confira a tabela acima." }
    if ($erros.Count -gt 0) {
        Write-Host "INCONSISTENTE: $($erros.Count) invariante(s) violado(s)." -ForegroundColor Red
        exit 1
    }
    Write-Host "CONSISTENTE: todos os invariantes valem." -ForegroundColor Green
    exit 0
}
finally {
    Pop-Location
}