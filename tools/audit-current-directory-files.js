/**
 * Repository-local adaptation of the audit workflow.
 *
 * The workflow runtime provides phase(), log(), agent(), and pipeline().
 * Set AUDIT_TARGET_DIR to the assigned directory before running it. The
 * default is the current working directory, so Windows example paths are
 * never treated as repository paths in this workspace.
 */
export const meta = {
  name: 'audit-current-directory-files',
  description: 'Audit every source file in the assigned repository directory with one cross-project agent per file',
  phases: [
    { title: 'Find Files', detail: 'Locate all eligible files below the assigned directory' },
    { title: 'Audit Code', detail: 'Run one independent cross-project audit agent for each file' },
    { title: 'Aggregate', detail: 'Combine findings and emit the directory-scoped issues report' }
  ]
}

const targetDir = process.env.AUDIT_TARGET_DIR || process.cwd()
const extensions = (process.env.AUDIT_EXTENSIONS || '.java,.kt,.kts,.json,.snbt,.toml,.mcmeta,.py,.js,.ts,.tsx')
  .split(',')
  .map(value => value.trim().toLowerCase())
  .filter(Boolean)

const excludedSegments = [
  '/build/', '/run/', '/run-data/', '/.gradle/', '/.git/', '/.bak/', '/backups/',
  '/node_modules/', '/__tests__/'
]

phase('Find Files')
log(`Finding eligible files under ${targetDir}`)
const files = await agent(`List every regular file below ${targetDir} whose extension is one of ${extensions.join(', ')}.
Exclude build output, runtime data, Gradle caches, Git metadata, .bak/backups, node_modules, __tests__,
and files whose basename ends with .test.ts, .spec.ts, Test.java, or Tests.java.
Return repository-relative paths where possible, sorted lexicographically, with no directories and no duplicates.`, {
  schema: {
    type: 'object',
    properties: {
      paths: { type: 'array', items: { type: 'string' } }
    },
    required: ['paths']
  }
})

const eligiblePaths = files.paths.filter(filePath => {
  const normalized = `/${filePath.replaceAll('\\', '/')}/`
  const lower = filePath.toLowerCase()
  return extensions.some(extension => lower.endsWith(extension))
    && !excludedSegments.some(segment => normalized.includes(segment))
    && !/(\.test\.(ts|tsx)|\.spec\.(ts|tsx)|Test\.java$|Tests\.java$)/.test(filePath)
})

log(`Found ${eligiblePaths.length} files to audit.`)

phase('Audit Code')
const results = await pipeline(
  eligiblePaths,
  async (filePath) => {
    log(`Auditing ${filePath}`)
    return await agent(`You are a senior software engineer and security auditor.
Start with the file at ${filePath}, then map every relevant caller, callee, data/resource,
registration, persistence boundary, network boundary, and test across the entire repository.
Do not limit the review to the target file and do not invent behavior that is not present.

Look for:
1. Logical bugs, incorrect state transitions, and unhandled edge cases.
2. Security or authority violations, especially client trust, ownership, replay, and persistence issues.
3. Cross-module contract mismatches, missing registration/resource wiring, and compatibility risks.
4. Performance problems, lifecycle leaks, and concurrency/reentrancy hazards.
5. Bad practices or code smells only when they create a concrete maintenance or correctness risk.

For every finding, give a precise description, impact, evidence path(s), and the target file line.
If the file is sound after the cross-project trace, return an empty findings array. Do not propose
changes merely because a different design would be preferable. Return JSON only.`, {
      label: `Audit ${filePath}`,
      phase: 'Audit Code',
      schema: {
        type: 'object',
        properties: {
          file: { type: 'string' },
          hasIssues: { type: 'boolean' },
          findings: {
            type: 'array',
            items: {
              type: 'object',
              properties: {
                type: { type: 'string', enum: ['bug', 'security', 'perf', 'smell'] },
                description: { type: 'string' },
                severity: { type: 'string', enum: ['low', 'medium', 'high', 'critical'] },
                lineNumber: { type: 'number' },
                evidence: { type: 'array', items: { type: 'string' } }
              },
              required: ['type', 'description', 'severity']
            }
          }
        },
        required: ['file', 'hasIssues', 'findings']
      }
    })
  }
)

phase('Aggregate')
log('Aggregating results...')
const allFindings = results.filter(Boolean)
const filesWithIssues = allFindings.filter(result => result.hasIssues && result.findings.length > 0)
const totalIssues = filesWithIssues.reduce((sum, result) => sum + result.findings.length, 0)

const reportPath = `issues/${targetDir.replaceAll('\\', '/').replace(/^\.\//, '').replace(/\/$/, '').replace(/[^a-zA-Z0-9._-]+/g, '-') || 'root'}.md`
const report = [
  `# 审计报告：${targetDir}`,
  '',
  `- 审计文件数：${allFindings.length}`,
  `- 有问题文件数：${filesWithIssues.length}`,
  `- 问题总数：${totalIssues}`,
  `- 规则：每个文件一个独立代理；每个代理从目标文件映射全项目调用链和资源边界。`,
  '',
  '## 发现',
  '',
  ...filesWithIssues.flatMap(result => [
    `### ${result.file}`,
    '',
    ...result.findings.map(finding => `- **${finding.severity} / ${finding.type}**${finding.lineNumber ? `（第 ${finding.lineNumber} 行）` : ''}：${finding.description}${finding.evidence?.length ? `\n  证据：${finding.evidence.join('、')}` : ''}`),
    ''
  ]),
  filesWithIssues.length ? '' : '未发现可由当前代码证据支持的具体问题。',
  '',
  `> 工作流输出路径：${reportPath}`
].join('\n')

return {
  summary: `Audited ${allFindings.length} files. Found ${totalIssues} issues across ${filesWithIssues.length} files.`,
  reportPath,
  report,
  issuesByFile: filesWithIssues
}
