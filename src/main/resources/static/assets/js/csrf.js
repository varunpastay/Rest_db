/* Spring Security's CSRF protection covers <form> submissions automatically (Thymeleaf's
   thymeleaf-extras-springsecurity6 dialect injects the token into every th:action form), but a
   plain fetch()/XHR POST has no such help - it needs the token attached as a request header by
   hand, or Spring Security rejects it with 403 before the controller ever runs. This reads the
   token Spring Security exposes via <meta> tags (see fragments/head.html) and returns the header
   to merge into a fetch() call's options. Returns {} harmlessly on pages where CSRF is disabled
   for the endpoint (the header is simply ignored) or the meta tags are absent. */
window.getCsrfHeaders = function () {
    var tokenMeta = document.querySelector('meta[name="_csrf"]');
    var headerMeta = document.querySelector('meta[name="_csrf_header"]');
    if (!tokenMeta || !headerMeta || !tokenMeta.content || !headerMeta.content) {
        return {};
    }
    var headers = {};
    headers[headerMeta.content] = tokenMeta.content;
    return headers;
};
