const listMaker = (list, e) => {
    const li = document.createElement('li')

    const wrapper = document.createElement('div')
    const date = document.createElement('h6')
    date.className = "date"

    const row = document.createElement('div')
    row.className = "row"
    const title = document.createElement('h2')
    const subtitle = document.createElement('h4')

    date.innerHTML = new Date(e.timestamp).toLocaleString()

    title.innerHTML = e.action
    let subtitleContent = ""
    if (e.target === undefined) {
        if (e.selector === undefined) {
            if (e.estimatedTweetsScrolled !== undefined) {
                subtitleContent = "est. tweets scrolled: " + e.estimatedTweetsScrolled
            }
        } else {
            if (e.selector.length <= 15) {
                subtitleContent = e.selector
            } else {
                subtitleContent = e.selector.substring(0, 15) + "…"
            }
        }
    } else {
        subtitleContent = e.target
    }
    subtitle.innerHTML = subtitleContent
    row.appendChild(title)
    row.appendChild(subtitle)

    wrapper.appendChild(date)
    wrapper.appendChild(row)

    li.appendChild(wrapper)

    list.appendChild(li)
}

let list = document.querySelector("#list");

browser.storage.local.get({events: []})
    .then(
        result => result.events.reverse().forEach(e => listMaker(list, e)),
        err => console.error("Couldn't load events:", err)
    )