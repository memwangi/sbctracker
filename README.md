## SBC Logistics App.

SBC deals in production and distribution of beverages, and is the sole distributor for Pepsi in Kenya. They have salespeople countrywide who monitor the stock status in fridges countrywide. They previously used to send photos on Whatsapp groups, and there were Whatsapp groups for designated regions. For instance, salespeople in Nairobi West sent pictures of fridges from various retails outlets in Nairobi West that get beverages from SBC. 

But this was inefficient because they couldn't make use of the data from various outlets. Plus conversations on Whatsapp can be overwhelming, especially when everyone is sending photos at the same time. 
Yet, the group supervisor had to keep taking stock as photos came in from various outlets. It was also hard to keep track of the salespeople's location, or automatically register the number of outlets they'd visited in a day. Lack of such data consequently made it hard to track their perfomance over time.

## The Solution
The client needed a system to help fix these flaws. I created the android app with that in mind and came up with the following features:


- Integrated GPS to send the user location every fifteen minutes, to a backend(built by my wonderful colleagues @csdigital), which the company uses as a web portal. I used workmanager + fused location provider to receive and schedule background location updates
- 
