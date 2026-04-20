export interface ForumPost {
  id?: number;
  topicId?: number;
  userId?: number;
  author?: string;
  username?: string;
  avatar?: string;
  content: string;
  image?: string;
  isEdited?: boolean;
  parentPostId?: number;
  sharedPostId?: number;
  comments?: number;
  reposts?: number;
  likes?: number;
  dislikes?: number;
  createdAt?: string | Date;
  updatedAt?: string | Date;
}
