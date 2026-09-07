// Package validate 对 OpenRTB JSON 执行嵌入式 JSON Schema 校验。
//
// ValidateBidRequest / ValidateBidResponse 返回统一的 ValidationResult，
// 可用于 HTTP 400 响应体或构建器 BuildValidated 流程。
package validate
