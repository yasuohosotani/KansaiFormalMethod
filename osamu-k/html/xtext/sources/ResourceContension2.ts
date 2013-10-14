model ResourceContension2  {
	transition system user1 {
		states { idle requesting critical releasing }
		events { request1 request2 release1 release2 }
		transitions {
			idle * request1 ->  requesting
			requesting * request2 -> critical
			critical * release1 -> releasing
			releasing * release2 -> idle
		}
		initial states { idle }
	}
	transition system user2 {
		states { idle requesting critical releasing }
		events { request1 request2 release1 release2 }
		transitions {
			idle * request1 ->  requesting
			requesting * request2 -> critical
			critical * release1 -> releasing
			releasing * release2 -> idle
		}
		initial states { idle }
	}
	transition system resource1 {
		states { unlock lock }
		events { request1 release1 }
		transitions {
			unlock * request1 -> lock
			lock * release1 -> unlock
		}
		initial states { unlock }
	}
	transition system resource2 {
		states { unlock lock }
		events { request2 release2 }
		transitions {
			unlock * request2 -> lock
			lock * release2 -> unlock
		}
		initial states { unlock }
	}
	main = (user1 ||| user2) || resource1 || resource2
}
