#!/usr/bin/env python3
"""Pretends to be a few people using the library, so Grafana has something to show."""

import http.cookies
import json
import os
import pathlib
import random
import signal
import threading
import time
import urllib.error
import urllib.parse
import urllib.request

# 1 is about 1 RPS in total, 10 about 10
SPEED = float(os.environ.get("SIMULATOR_SPEED", "1"))
BASE_URL = os.environ.get("SIMULATOR_BASE_URL", "http://localhost:8080")
ADMIN_EMAIL = os.environ.get("LIBRARY_ADMIN_EMAIL", "admin@example.com")
ADMIN_PASSWORD = os.environ.get("LIBRARY_ADMIN_PASSWORD", "change-me")

VISITORS = 2
READERS = 4
DESK_LIBRARIANS = 2
CATALOG_LIBRARIANS = 1

# Seconds at speed 1
BETWEEN_REQUESTS = (1, 5)
BETWEEN_VISITS = (15, 60)

MAX_CUSTOMERS = 60
MAX_RESERVATIONS = 3
MAX_BORROWED = 3

EMAIL_DOMAIN = "@simulator.example"
PASSWORD = "simulator-password"
# Made up: the server only checks that the header equals the cookie
CSRF_TOKEN = "simulator-token"

FIRST_NAMES = ["Anna", "Piotr", "Maria", "Jan", "Ewa", "Tomasz", "Zofia", "Adam", "Julia", "Marek", "Olga", "Karol"]
LAST_NAMES = ["Nowak", "Kowalski", "Lewandowska", "Zielinski", "Kaminska", "Wojcik", "Mazur", "Krawczyk", "Dudek",
              "Pawlak", "Sikora", "Baran"]

BOOKS = json.loads(pathlib.Path(__file__).with_name("books.json").read_text())

# What people type into a search box
TITLE_WORDS = [word for book in BOOKS for word in book["title"].split() if len(word) > 3]
AUTHOR_SURNAMES = [name.split()[-1] for book in BOOKS for name in book["authorNames"]]

LOG_LOCK = threading.Lock()


def log(name, message):
    # One thread at a time, or lines interleave
    with LOG_LOCK:
        print(f"{time.strftime('%H:%M:%S')} {name:<8} {message}", flush=True)


def pause(seconds):
    time.sleep(random.uniform(*seconds) / SPEED)


def random_name():
    return {"firstName": random.choice(FIRST_NAMES), "lastName": random.choice(LAST_NAMES)}


class Client:
    """One person's browser."""

    def __init__(self, name):
        self.name = name
        self.session_cookie = None

    def request(self, method, path, params=None, json_body=None, form=None):
        """Returns (status, body). Error statuses are printed, not raised."""
        url = BASE_URL + path
        if params:
            url += "?" + urllib.parse.urlencode(params)

        cookie = f"XSRF-TOKEN={CSRF_TOKEN}"
        if self.session_cookie:
            cookie += f"; JSESSIONID={self.session_cookie}"
        headers = {"Cookie": cookie, "X-XSRF-TOKEN": CSRF_TOKEN}

        data = None
        if json_body is not None:
            data = json.dumps(json_body).encode()
            headers["Content-Type"] = "application/json"
        if form is not None:
            data = urllib.parse.urlencode(form).encode()
            headers["Content-Type"] = "application/x-www-form-urlencoded"

        request = urllib.request.Request(url, data=data, headers=headers, method=method)
        try:
            response = urllib.request.urlopen(request, timeout=30)
        except urllib.error.HTTPError as error:
            # A 4xx or 5xx is still a response
            response = error

        with response:
            status = response.status
            text = response.read().decode()
            for header in response.headers.get_all("Set-Cookie", []):
                cookies = http.cookies.SimpleCookie(header)
                if "JSESSIONID" in cookies:
                    self.session_cookie = cookies["JSESSIONID"].value

        body = json.loads(text) if text else None
        if status >= 400:
            detail = body.get("detail", "") if body else ""
            log(self.name, f"{method} {path} -> {status} {detail}")
        return status, body

    def login(self, email, password):
        status, _ = self.request("POST", "/api/login", form={"email": email, "password": password})
        return status == 200

    def logout(self):
        if self.session_cookie:
            self.request("POST", "/api/logout")
            self.session_cookie = None


def admin_client(name):
    """The bootstrap librarian, logged in, or None."""
    admin = Client(name)
    if admin.login(ADMIN_EMAIL, ADMIN_PASSWORD):
        return admin
    log(name, "bootstrap librarian login failed: check LIBRARY_ADMIN_EMAIL and LIBRARY_ADMIN_PASSWORD")
    return None


def login_reader(client):
    """Logs in, signing up first if the account doesn't exist yet."""
    email = client.name + EMAIL_DOMAIN
    if client.login(email, PASSWORD):
        return True

    # A librarian creates the customer and hands over an activation code
    admin = admin_client(client.name)
    if admin is None:
        return False
    status, customer = admin.request("POST", "/api/admin/customers", json_body=random_name())
    if status == 201:
        status, code = admin.request("POST", f"/api/admin/customers/{customer['id']}/activation-code")
    admin.logout()
    if status != 201:
        return False

    registration = {"activationCode": code["activationCode"], "email": email, "password": PASSWORD}
    status, _ = client.request("POST", "/api/register", json_body=registration)
    if status != 201:
        return False
    log(client.name, f"registered as {email}")
    return client.login(email, PASSWORD)


def login_librarian(client):
    """Logs in, getting an account from the bootstrap librarian first if there is none yet."""
    email = client.name + EMAIL_DOMAIN
    if client.login(email, PASSWORD):
        return True

    admin = admin_client(client.name)
    if admin is None:
        return False
    status, _ = admin.request("POST", "/api/admin/librarians", json_body={"email": email, "password": PASSWORD})
    admin.logout()
    if status != 201:
        return False
    log(client.name, f"got the librarian account {email}")
    return client.login(email, PASSWORD)


def browse(client):
    """A visitor's whole visit: looks around the catalog. Returns the books seen in lists."""
    seen = []

    # Mostly the first page
    page = random.choice([0, 0, 0, 1, 2])
    status, body = client.request("GET", "/api/books", params={"page": page})
    if status == 200:
        seen += body["content"]
    pause(BETWEEN_REQUESTS)

    if random.random() < 0.5:
        word = random.choice(TITLE_WORDS + AUTHOR_SURNAMES)
        status, body = client.request("GET", "/api/books/search", params={"q": word})
        if status == 200:
            seen += body["content"]
        pause(BETWEEN_REQUESTS)

    for book in random.sample(seen, min(len(seen), random.randint(1, 3))):
        client.request("GET", f"/api/books/{book['isbn']}")
        pause(BETWEEN_REQUESTS)

    if random.random() < 0.3:
        status, body = client.request("GET", "/api/authors")
        pause(BETWEEN_REQUESTS)
        if status == 200 and body["content"]:
            author = random.choice(body["content"])
            client.request("GET", "/api/authors/" + urllib.parse.quote(author["name"]))
            pause(BETWEEN_REQUESTS)

    return seen


def reader_visit(client):
    if not login_reader(client):
        return
    pause(BETWEEN_REQUESTS)

    status, body = client.request("GET", "/api/reservations/mine")
    if status != 200:
        return
    reserved = [copy for copy in body["content"] if copy["status"] == "RESERVED"]
    pause(BETWEEN_REQUESTS)

    if reserved and random.random() < 0.2:
        copy = random.choice(reserved)
        status, _ = client.request("DELETE", f"/api/reservations/{copy['id']}")
        if status == 204:
            reserved.remove(copy)
            log(client.name, f'cancelled the reservation of "{copy["book"]["title"]}"')
        pause(BETWEEN_REQUESTS)

    available = [book for book in browse(client) if book["availableCopies"] > 0]
    if available and len(reserved) < MAX_RESERVATIONS and random.random() < 0.6:
        book = random.choice(available)
        status, _ = client.request("POST", "/api/reservations", json_body={"bookIsbn": book["isbn"]})
        if status == 201:
            log(client.name, f'reserved "{book["title"]}"')
        pause(BETWEEN_REQUESTS)


def desk_visit(client):
    if not login_librarian(client):
        return
    for _ in range(random.randint(1, 3)):
        pause(BETWEEN_REQUESTS)
        serve_patron(client)


def serve_patron(client):
    """Someone walks up to the desk: hand over their reservations, take returns, maybe lend a book."""
    # One page holds everyone: MAX_CUSTOMERS is below the page size
    status, body = client.request("GET", "/api/admin/customers", params={"size": 100})
    if status != 200:
        return
    customers = body["content"]
    pause(BETWEEN_REQUESTS)

    is_new_patron = len(customers) < MAX_CUSTOMERS and random.random() < 0.1
    if not customers or is_new_patron:
        status, customer = client.request("POST", "/api/admin/customers", json_body=random_name())
        if status != 201:
            return
        log(client.name, f"signed up {customer['firstName']} {customer['lastName']}")
    else:
        customer = random.choice(customers)
    patron = f"{customer['firstName']} {customer['lastName']}"
    pause(BETWEEN_REQUESTS)

    status, body = client.request("GET", "/api/admin/copies", params={"customerId": customer["id"], "size": 100})
    if status != 200:
        return
    pause(BETWEEN_REQUESTS)

    borrowed = 0
    for copy in body["content"]:
        title = copy["book"]["title"]
        desk_request = {"copyId": copy["id"], "customerId": customer["id"]}

        if copy["status"] == "RESERVED":
            status, _ = client.request("POST", "/api/desk/checkout", json_body=desk_request)
            if status == 200:
                borrowed += 1
                log(client.name, f'{patron} picked up "{title}"')
            pause(BETWEEN_REQUESTS)

        elif copy["status"] == "BORROWED":
            roll = random.random()
            if roll < 0.005:
                status, _ = client.request("POST", "/api/desk/mark-lost", json_body={"copyId": copy["id"]})
                if status == 200:
                    log(client.name, f'{patron} lost "{title}"')
                pause(BETWEEN_REQUESTS)
            elif roll < 0.5:
                status, _ = client.request("POST", "/api/desk/return", json_body=desk_request)
                if status == 200:
                    log(client.name, f'{patron} returned "{title}"')
                pause(BETWEEN_REQUESTS)
            else:
                borrowed += 1

    if borrowed < MAX_BORROWED and random.random() < 0.5:
        lend_from_shelf(client, customer, patron)


def lend_from_shelf(client, customer, patron):
    word = random.choice(TITLE_WORDS)
    status, body = client.request("GET", "/api/admin/copies", params={"title": word, "size": 100})
    if status != 200:
        return
    available = [copy for copy in body["content"] if copy["status"] == "AVAILABLE"]
    pause(BETWEEN_REQUESTS)

    if available:
        copy = random.choice(available)
        desk_request = {"copyId": copy["id"], "customerId": customer["id"]}
        status, _ = client.request("POST", "/api/desk/checkout", json_body=desk_request)
        if status == 200:
            log(client.name, f'{patron} borrowed "{copy["book"]["title"]}"')
        pause(BETWEEN_REQUESTS)


def catalog_visit(client):
    if not login_librarian(client):
        return
    # A delivery of a few books
    for book in random.sample(BOOKS, random.randint(2, 5)):
        pause(BETWEEN_REQUESTS)
        shelve(client, book)


def shelve(client, book):
    isbn = book["isbn"]
    title = book["title"]
    status, existing = client.request("GET", f"/api/books/{isbn}")
    pause(BETWEEN_REQUESTS)

    if status == 404:
        status, _ = client.request("POST", "/api/admin/books", json_body=book)
        if status != 201:
            return
        pause(BETWEEN_REQUESTS)
        if random.random() < 0.1:
            status, _ = client.request("DELETE", f"/api/admin/books/{isbn}")
            if status == 204:
                log(client.name, f'added "{title}", then removed it: the order was cancelled')
        else:
            quantity = random.randint(1, 4)
            status, _ = client.request("POST", "/api/admin/copies", json_body={"bookIsbn": isbn, "quantity": quantity})
            if status == 201:
                log(client.name, f'added "{title}" with {quantity} copies')

    elif status == 200 and existing["availableCopies"] == 0:
        quantity = random.randint(1, 2)
        status, _ = client.request("POST", "/api/admin/copies", json_body={"bookIsbn": isbn, "quantity": quantity})
        if status == 201:
            log(client.name, f'ordered {quantity} more copies of "{title}"')

    elif status == 200 and random.random() < 0.3:
        # Same data: only there so the update endpoint gets traffic
        record = {"title": title, "publicationYear": book["publicationYear"], "authorNames": book["authorNames"]}
        status, _ = client.request("PUT", f"/api/admin/books/{isbn}", json_body=record)
        if status == 200:
            log(client.name, f're-saved the record of "{title}"')


def run(name, visit):
    """One person: visit, leave, come back later, forever."""
    client = Client(name)
    # So they don't all arrive at once
    pause((0, 10))
    while True:
        try:
            visit(client)
            client.logout()
        except Exception as error:
            # App down, restarted or wiped: try again next visit
            client.session_cookie = None
            log(name, f"visit failed: {error}")
        pause(BETWEEN_VISITS)


def start(name, visit):
    threading.Thread(target=run, args=(name, visit), daemon=True).start()


def main():
    # Treat `docker stop` like Ctrl+C. Without a handler, SIGTERM is ignored in a container.
    signal.signal(signal.SIGTERM, signal.default_int_handler)

    log("main", f"simulating against {BASE_URL} at speed {SPEED}")
    for number in range(1, VISITORS + 1):
        start(f"visitor{number}", browse)
    for number in range(1, READERS + 1):
        start(f"reader{number}", reader_visit)
    for number in range(1, DESK_LIBRARIANS + 1):
        start(f"desk{number}", desk_visit)
    for number in range(1, CATALOG_LIBRARIANS + 1):
        start(f"catalog{number}", catalog_visit)

    try:
        while True:
            time.sleep(3600)
    except KeyboardInterrupt:
        pass


if __name__ == "__main__":
    main()
