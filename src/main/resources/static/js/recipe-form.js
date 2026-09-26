document.addEventListener('DOMContentLoaded', function () {
    var tbody = document.getElementById('ingredients-rows');
    var template = document.getElementById('ingredient-row');
    var addButton = document.getElementById('add-ingredient');
    if (!tbody || !template || !addButton) {
        return;
    }

    function reindex() {
        Array.prototype.forEach.call(tbody.querySelectorAll('tr'), function (row, index) {
            Array.prototype.forEach.call(row.querySelectorAll('[name]'), function (element) {
                element.name = element.name.replace(/ingredients\[\d+]/, 'ingredients[' + index + ']');
            });
        });
    }

    addButton.addEventListener('click', function () {
        var index = tbody.querySelectorAll('tr').length;
        var wrapper = document.createElement('tbody');
        wrapper.innerHTML = template.innerHTML.replace(/__INDEX__/g, index).trim();
        tbody.appendChild(wrapper.firstElementChild);
    });

    tbody.addEventListener('click', function (event) {
        if (event.target.classList.contains('remove-row')) {
            event.target.closest('tr').remove();
            reindex();
        }
    });
});
