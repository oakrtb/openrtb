// Package inspect 提供轻量级 BidRequest / BidResponse 视图流水线：
// LightGate → SharedView → ImpView/BidView，以及 RequestPipeline / ResponsePipeline
// 编排并在 Snapshot 中产出可查询结果。
//
// 不执行完整 JSON Schema 校验——请使用 validate 包。
//
// 命名说明：inspect.MarkupMask 表示 Imp 上的 markup 类型位掩码，与 proto 中的
// Format（Banner 尺寸条目）不同；inspect.Inventory 表示请求级库存面（site/app/dooh），
// 与 proto Content.Channel（内容分发渠道）不同。
package inspect
