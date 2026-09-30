"use strict";

const state = {
  products: [],
  inventories: new Map(),
  selectedProductId: null,
  currentOrder: null
};

class ApiError extends Error {
  constructor(response, problem) {
    super(problem?.detail || `요청 처리에 실패했다. (HTTP ${response.status})`);
    this.status = response.status;
    this.problem = problem;
  }
}

const elements = {
  apiStatus: document.querySelector("#api-status"),
  notice: document.querySelector("#notice"),
  productForm: document.querySelector("#product-form"),
  productRows: document.querySelector("#product-rows"),
  productEmpty: document.querySelector("#product-empty"),
  refreshProducts: document.querySelector("#refresh-products"),
  selectedProduct: document.querySelector("#selected-product"),
  onHandQuantity: document.querySelector("#on-hand-quantity"),
  reservedQuantity: document.querySelector("#reserved-quantity"),
  availableQuantity: document.querySelector("#available-quantity"),
  receiptForm: document.querySelector("#receipt-form"),
  orderForm: document.querySelector("#order-form"),
  orderItems: document.querySelector("#order-items"),
  addOrderItem: document.querySelector("#add-order-item"),
  orderLookupForm: document.querySelector("#order-lookup-form"),
  orderDetail: document.querySelector("#order-detail"),
  movementFilterForm: document.querySelector("#movement-filter-form"),
  movementRows: document.querySelector("#movement-rows"),
  movementEmpty: document.querySelector("#movement-empty"),
  refreshMovements: document.querySelector("#refresh-movements")
};

async function request(path, options = {}) {
  const headers = new Headers(options.headers || {});
  if (options.body) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(path, {...options, headers});
  const contentType = response.headers.get("content-type") || "";
  const body = contentType.includes("json") ? await response.json() : null;

  if (!response.ok) {
    throw new ApiError(response, body);
  }
  return body;
}

function showNotice(message, kind = "success") {
  elements.notice.textContent = message;
  elements.notice.className = `notice notice-${kind}`;
  elements.notice.hidden = false;
  window.clearTimeout(showNotice.timeoutId);
  showNotice.timeoutId = window.setTimeout(() => {
    elements.notice.hidden = true;
  }, 6000);
}

function formatError(error) {
  if (!(error instanceof ApiError)) {
    return "요청 중 예상하지 못한 오류가 발생했다.";
  }

  const parts = [];
  if (error.problem?.errorCode) {
    parts.push(`[${error.problem.errorCode}]`);
  }
  parts.push(error.message);

  const fieldErrors = error.problem?.fieldErrors || [];
  if (fieldErrors.length > 0) {
    parts.push(fieldErrors.map((item) => `${item.field}: ${item.message}`).join("\n"));
  }
  return parts.join(" ");
}

function handleError(error) {
  showNotice(formatError(error), "error");
  console.error(error);
}

async function withPending(form, action) {
  const buttons = form.querySelectorAll("button");
  buttons.forEach((button) => {
    button.disabled = true;
  });
  try {
    await action();
  } catch (error) {
    handleError(error);
  } finally {
    buttons.forEach((button) => {
      button.disabled = false;
    });
  }
}

function createCell(value, className) {
  const cell = document.createElement("td");
  cell.textContent = value;
  if (className) {
    cell.className = className;
  }
  return cell;
}

function formatDateTime(value) {
  if (!value) {
    return "-";
  }
  return new Intl.DateTimeFormat("ko-KR", {
    dateStyle: "short",
    timeStyle: "medium"
  }).format(new Date(value));
}

function formatDelta(value) {
  return value > 0 ? `+${value}` : String(value);
}

async function checkHealth() {
  try {
    const health = await request("/actuator/health");
    elements.apiStatus.textContent = health.status === "UP" ? "API 정상" : "API 확인 필요";
    elements.apiStatus.className = health.status === "UP"
      ? "status-pill status-ok"
      : "status-pill status-error";
  } catch (error) {
    elements.apiStatus.textContent = "API 연결 실패";
    elements.apiStatus.className = "status-pill status-error";
  }
}

async function loadProducts(options = {}) {
  const page = await request("/api/v1/products?page=0&size=100");
  state.products = page.content;

  const inventoryEntries = await Promise.all(state.products.map(async (product) => {
    try {
      return [product.id, await request(`/api/v1/inventories/${product.id}`)];
    } catch (error) {
      return [product.id, null];
    }
  }));
  state.inventories = new Map(inventoryEntries);

  renderProducts();
  refreshOrderProductOptions();

  if (options.selectProductId) {
    await selectProduct(options.selectProductId);
  } else if (state.selectedProductId) {
    await selectProduct(state.selectedProductId);
  }
}

function renderProducts() {
  elements.productRows.replaceChildren();
  elements.productEmpty.hidden = state.products.length > 0;

  state.products.forEach((product) => {
    const inventory = state.inventories.get(product.id);
    const row = document.createElement("tr");
    row.append(
      createCell(product.id),
      createCell(product.sku),
      createCell(product.name),
      createCell(inventory ? inventory.availableQuantity : "-")
    );

    const actionCell = document.createElement("td");
    const button = document.createElement("button");
    button.type = "button";
    button.className = "button button-small button-ghost";
    button.textContent = state.selectedProductId === product.id ? "선택됨" : "재고 선택";
    button.disabled = state.selectedProductId === product.id;
    button.addEventListener("click", () => selectProduct(product.id).catch(handleError));
    actionCell.append(button);
    row.append(actionCell);
    elements.productRows.append(row);
  });
}

async function selectProduct(productId) {
  const product = state.products.find((item) => item.id === Number(productId));
  if (!product) {
    return;
  }

  const inventory = await request(`/api/v1/inventories/${product.id}`);
  state.selectedProductId = product.id;
  state.inventories.set(product.id, inventory);

  elements.selectedProduct.textContent = `${product.sku} · ${product.name}`;
  elements.onHandQuantity.textContent = inventory.onHandQuantity;
  elements.reservedQuantity.textContent = inventory.reservedQuantity;
  elements.availableQuantity.textContent = inventory.availableQuantity;
  elements.receiptForm.querySelector("input").disabled = false;
  elements.receiptForm.querySelector("button").disabled = false;
  elements.movementFilterForm.elements.productId.value = product.id;
  renderProducts();
}

function optionForProduct(product) {
  const option = document.createElement("option");
  option.value = product.id;
  option.textContent = `${product.sku} · ${product.name}`;
  return option;
}

function refreshOrderProductOptions() {
  elements.orderItems.querySelectorAll("select").forEach((select) => {
    const currentValue = select.value;
    select.replaceChildren();

    const placeholder = document.createElement("option");
    placeholder.value = "";
    placeholder.textContent = "상품을 선택한다";
    select.append(placeholder, ...state.products.map(optionForProduct));
    select.value = currentValue;
  });
}

function addOrderItemRow() {
  const row = document.createElement("div");
  row.className = "order-item-row";

  const productLabel = document.createElement("label");
  productLabel.textContent = "상품";
  const select = document.createElement("select");
  select.name = "productId";
  select.required = true;
  const placeholder = document.createElement("option");
  placeholder.value = "";
  placeholder.textContent = "상품을 선택한다";
  select.append(placeholder, ...state.products.map(optionForProduct));
  productLabel.append(select);

  const quantityLabel = document.createElement("label");
  quantityLabel.textContent = "수량";
  const quantity = document.createElement("input");
  quantity.name = "quantity";
  quantity.type = "number";
  quantity.min = "1";
  quantity.step = "1";
  quantity.placeholder = "1";
  quantity.required = true;
  quantityLabel.append(quantity);

  const remove = document.createElement("button");
  remove.type = "button";
  remove.className = "button button-small button-danger";
  remove.textContent = "삭제";
  remove.addEventListener("click", () => {
    if (elements.orderItems.childElementCount > 1) {
      row.remove();
    } else {
      showNotice("주문에는 하나 이상의 항목이 필요하다.", "error");
    }
  });

  row.append(productLabel, quantityLabel, remove);
  elements.orderItems.append(row);
}

function orderPayload() {
  const rows = elements.orderItems.querySelectorAll(".order-item-row");
  return {
    items: Array.from(rows).map((row) => ({
      productId: Number(row.querySelector("select").value),
      quantity: Number(row.querySelector("input").value)
    }))
  };
}

function statusText(status) {
  return {
    RESERVED: "예약",
    SHIPPED: "출고 완료",
    CANCELLED: "취소"
  }[status] || status;
}

function renderOrder(order) {
  state.currentOrder = order;
  elements.orderDetail.replaceChildren();
  elements.orderDetail.className = "order-detail order-summary";

  const head = document.createElement("div");
  head.className = "order-summary-head";
  const title = document.createElement("h3");
  title.textContent = `주문 #${order.id}`;
  const badge = document.createElement("span");
  badge.className = `status-badge status-${order.status.toLowerCase()}`;
  badge.textContent = statusText(order.status);
  head.append(title, badge);

  const list = document.createElement("ul");
  order.items.forEach((item) => {
    const line = document.createElement("li");
    line.textContent = `${item.sku} · ${item.quantity}개`;
    list.append(line);
  });

  const meta = document.createElement("p");
  meta.className = "selection-label";
  meta.textContent = `생성 ${formatDateTime(order.createdAt)}`;

  elements.orderDetail.append(head, list, meta);

  if (order.status === "RESERVED") {
    const actions = document.createElement("div");
    actions.className = "order-actions";
    const shipButton = document.createElement("button");
    shipButton.type = "button";
    shipButton.className = "button button-primary";
    shipButton.textContent = "출고 완료";
    shipButton.addEventListener("click", () => transitionOrder(order.id, "ship"));

    const cancelButton = document.createElement("button");
    cancelButton.type = "button";
    cancelButton.className = "button button-danger";
    cancelButton.textContent = "주문 취소";
    cancelButton.addEventListener("click", () => transitionOrder(order.id, "cancel"));
    actions.append(shipButton, cancelButton);
    elements.orderDetail.append(actions);
  }
}

async function transitionOrder(orderId, action) {
  try {
    const order = await request(`/api/v1/outbound-orders/${orderId}/${action}`, {method: "POST"});
    renderOrder(order);
    await Promise.all([loadProducts(), loadMovements()]);
    showNotice(action === "ship" ? "출고를 완료했다." : "주문을 취소했다.");
  } catch (error) {
    handleError(error);
  }
}

function movementTypeText(type) {
  return {
    RECEIPT: "입고",
    RESERVATION: "예약",
    SHIPMENT: "출고",
    RESERVATION_RELEASE: "예약 해제"
  }[type] || type;
}

async function loadMovements() {
  const form = new FormData(elements.movementFilterForm);
  const params = new URLSearchParams({page: "0", size: "100"});
  for (const name of ["productId", "orderId", "type"]) {
    const value = String(form.get(name) || "").trim();
    if (value) {
      params.set(name, value);
    }
  }

  const page = await request(`/api/v1/stock-movements?${params}`);
  elements.movementRows.replaceChildren();
  elements.movementEmpty.hidden = page.content.length > 0;

  page.content.forEach((movement) => {
    const row = document.createElement("tr");
    const onHandClass = movement.onHandDelta < 0 ? "quantity-negative" : "quantity-positive";
    const reservedClass = movement.reservedDelta < 0 ? "quantity-negative" : "quantity-positive";
    row.append(
      createCell(formatDateTime(movement.occurredAt)),
      createCell(movementTypeText(movement.type), "movement-type"),
      createCell(movement.productId),
      createCell(movement.orderId ?? "-"),
      createCell(formatDelta(movement.onHandDelta), onHandClass),
      createCell(formatDelta(movement.reservedDelta), reservedClass),
      createCell(movement.availableAfter)
    );
    elements.movementRows.append(row);
  });
}

elements.productForm.addEventListener("submit", (event) => {
  event.preventDefault();
  withPending(elements.productForm, async () => {
    const form = new FormData(elements.productForm);
    const product = await request("/api/v1/products", {
      method: "POST",
      body: JSON.stringify({sku: form.get("sku"), name: form.get("name")})
    });
    elements.productForm.reset();
    await Promise.all([
      loadProducts({selectProductId: product.id}),
      loadMovements()
    ]);
    showNotice(`${product.sku} 상품을 등록했다.`);
  });
});

elements.receiptForm.addEventListener("submit", (event) => {
  event.preventDefault();
  withPending(elements.receiptForm, async () => {
    if (!state.selectedProductId) {
      throw new Error("입고할 상품을 먼저 선택해야 한다.");
    }
    const form = new FormData(elements.receiptForm);
    await request(`/api/v1/inventories/${state.selectedProductId}/receipts`, {
      method: "POST",
      body: JSON.stringify({quantity: Number(form.get("quantity"))})
    });
    elements.receiptForm.reset();
    await Promise.all([
      loadProducts({selectProductId: state.selectedProductId}),
      loadMovements()
    ]);
    showNotice("입고를 처리했다.");
  });
});

elements.orderForm.addEventListener("submit", (event) => {
  event.preventDefault();
  withPending(elements.orderForm, async () => {
    const order = await request("/api/v1/outbound-orders", {
      method: "POST",
      body: JSON.stringify(orderPayload())
    });
    elements.orderLookupForm.elements.orderId.value = order.id;
    renderOrder(order);
    await Promise.all([loadProducts(), loadMovements()]);
    showNotice(`주문 #${order.id}을 생성하고 재고를 예약했다.`);
  });
});

elements.orderLookupForm.addEventListener("submit", (event) => {
  event.preventDefault();
  withPending(elements.orderLookupForm, async () => {
    const orderId = new FormData(elements.orderLookupForm).get("orderId");
    const order = await request(`/api/v1/outbound-orders/${orderId}`);
    renderOrder(order);
  });
});

elements.movementFilterForm.addEventListener("submit", (event) => {
  event.preventDefault();
  loadMovements().catch(handleError);
});

elements.refreshProducts.addEventListener("click", () => loadProducts().catch(handleError));
elements.refreshMovements.addEventListener("click", () => loadMovements().catch(handleError));
elements.addOrderItem.addEventListener("click", addOrderItemRow);

async function initialize() {
  addOrderItemRow();
  await checkHealth();
  try {
    await Promise.all([loadProducts(), loadMovements()]);
  } catch (error) {
    handleError(error);
  }
}

initialize();
