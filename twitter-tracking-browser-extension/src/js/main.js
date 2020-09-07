import {finder} from "@medv/finder";
import browser from "webextension-polyfill";
import sha256 from 'crypto-js/sha256';

const TARGETS = {
    like: selector => /#tweet-action-buttons.+:nth-child\(3\).*/.test(selector),
    retweet: selector => /\[data-testid=retweetConfirm\]/.test(selector) || /#tweet-action-buttons.+:nth-child\(2\).*/.test(selector),
    posting: selector => /div:nth-child\(4\)\s>\sdiv\s>\sspan/.test(selector),
    followByTweet: selector => /.*div:nth-child\(3\)\s>\sdiv\s>\sdiv\s>\sdiv\s>\sdiv:nth-child\(1\).*/.test(selector),
    follow: selector => /.*div:nth-child\([0-9]+\)\s>\sdiv:nth-child\(1\)\s>\sdiv:nth-child\(1\)\s>\sdiv:nth-child\(2\).*span:nth-child\(1\)/.test(selector)
        || /div:nth-child\([0-9]+\)\s>\sdiv\s>\sdiv\s>\sdiv\s>\sdiv:nth-child\(2\)\s>\sdiv\s>\sdiv:nth-child\(2\)\s>\sdiv/.test(selector)
        || /div:nth-child\([0-9]+\)[\s>\sdiv]{7}/.test(selector),
    clickOnMedia: selector => /#tweet-rich-content-label/.test(selector)  // mostly everything
        || /#card-detail-label/.test(selector)  // links
        || /#card-media-label/.test(selector)  // links
        || /\[data-testid=card\.layoutLarge\.detail\]/.test(selector) // links
        || /^div:nth-child\(3\).*img$/.test(selector) // links
        || /^div:nth-child\(3\).*\sa\s.*$/.test(selector) // links
        || /div:nth-child\(2\)\s>\sdiv\s>\sdiv\s>\sdiv\s>\sdiv:nth-child\(2\)\s>\sdiv\s>\sdiv:nth-child\(2\)\s>\sdiv\s>\sdiv:nth-child\(1\)/.test(selector), // video
    clickOnHashtag: selector => /#tweet-text.*a$/.test(selector), // must come before 'openDetailsView'
    openDetailsView: selector => /#tweet-text/.test(selector),
    visitAuthorsProfile: selector => /div:nth-child\([0-9]+\)\sa:nth-child\(1\)\s>\sdiv:nth-child\(2\)/.test(selector)
        || /^#tweet-user-name.*/.test(selector)
}

function mapTarget(selector) {
    return Object.keys(TARGETS).find(key => TARGETS[key](selector))
}

function parseEvent(event) {
    let selector = finder(event.target, {
        className: name => false // do not rely on classnames
    });

    let target = mapTarget(selector);

    if (!/compose\/tweet$/.test(window.location) && target === TARGETS.posting.name) {
        target = undefined
    }
    if (!/search.*&f=user/.test(window.location) && target === TARGETS.follow.name) {
        target = undefined
    }
    if (!/search/.test(window.location) && target === TARGETS.followByTweet.name) {
        target = undefined
    }

    return {
        action: event.type,
        target,
        selector
    };
}

function handleEvent(event) {
    if (event.isTrusted === true) publish(parseEvent(event));
}

let lastScroll = 0;

function scrolling(event) {
    if (event.isTrusted === true) {
        if (((document.documentElement.scrollTop) - lastScroll) > 300) {
            lastScroll = document.documentElement.scrollTop
            publish({
                action: "scroll",
                scrollPosition: lastScroll,
                estimatedTweetsScrolled: Math.floor(lastScroll / 300)
            })
        }
    }
}

function aboutToUnload() {
    publish({
        action: "session_end"
    })
}

function addListener(type, func) {
    document.addEventListener(type, func, {
        capture: true,
        passive: true,
    });
}

function send(event) {
    fetch(API, {
        method: "POST",
        headers: {
            'Content-Type': 'application/json'
        },
        mode: 'cors',
        body: JSON.stringify(event)
    })
        .then(
            async (response) => {
                if (response.status >= 200 && response.status <= 299) {
                    console.log("Result:", await response.text());
                } else {
                    throw Error(response.statusText);
                }
            },
            reason => console.error("Error while sending:", JSON.stringify(reason)))
}

function store(event) {
    browser.storage.local.get({events: []})
        .then(async result => {
            let events = [...result.events, event];
            await browser.storage.local.set({events})
        }, err => {
            console.error("Error while storing:", err)
        })
}

function publish(event) {
    if (userId === UNKNOWN_USER_ID)
        userId = hashUserName()

    event = Object.assign({}, event, {
        eventType: "BROWSER",
        userId,
        timestamp: new Date().toISOString()
    })

    console.log(event)

    store(event)
    send(event);
}

function hashUserName() {
    let profileImage = document.querySelector("div[data-testid=\"primaryColumn\"] > div > div:nth-child(2) a img");
    if (profileImage != null) {
        return sha256(profileImage.getAttribute("alt")).toString()
    } else {
        return UNKNOWN_USER_ID
    }
}

const UNKNOWN_USER_ID = "unknown_user"
let userId = hashUserName()

addListener("click", handleEvent)
addListener("scroll", scrolling)
window.addEventListener("beforeunload", aboutToUnload)

window.addEventListener("load", async _ => {
    await browser.storage.local.clear()
    publish({action: "session_start"})
})