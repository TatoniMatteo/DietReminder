export interface Account {
  userId: string
  email: string
  username: string
  role: 'USER' | 'ADMIN'
  googleLinked: boolean
  emailVerified?: boolean
  enrollmentStatus?: 'PENDING_EMAIL' | 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED'
  permissions?: import('./access').Permission[]
  reviewReason?: string | null
  version?: number
  adminIntroductionPending?: boolean
}
export interface WebSession {
  account: Account
  csrfToken: string
}
export interface Providers {
  firstRegistrationRequired?: boolean
  clientId: string
  signInEnabled: boolean
  driveEnabled: boolean
}
export type MealType =
  'BREAKFAST' | 'MORNING_SNACK' | 'LUNCH' | 'AFTERNOON_SNACK' | 'DINNER' | 'OTHER'
export type Unit =
  | 'GRAMS'
  | 'KILOGRAMS'
  | 'MILLIGRAMS'
  | 'MILLILITERS'
  | 'CENTILITERS'
  | 'DECILITERS'
  | 'LITERS'
  | 'PIECES'
  | 'SLICES'
  | 'CLOVES'
  | 'SPOONS'
  | 'TEASPOONS'
  | 'CUPS'
  | 'GLASSES'
  | 'PINCHES'
  | 'JARS'
  | 'CANS'
  | 'PACKETS'
  | 'LEAVES'
  | 'SPRIGS'
  | 'DROPS'
  | 'PORTIONS'
  | 'QB'
  | 'FREE'
  | 'CUSTOM'
export interface Diet {
  id: number
  uuid: string
  name: string
  nextMealWindowMinutes: number
  isActive: boolean
  disabledNotificationDays: string
}
export interface Meal {
  id: number
  dietId: number
  dayOfWeek: number
  type: MealType
  timeMinutes: number
  description: string
  customTypeLabel: string | null
  isNotificationEnabled: boolean
}
export interface Course {
  id: number
  mealId: number
  name: string
  order: number
}
export interface Food {
  id: number
  courseId: number
  name: string
  amount: string
  unit: Unit
  order: number
}
export interface FoodCategory {
  name: string
  keywords: string[]
}
export interface ShoppingList {
  remindersEnabled?: boolean
  reminderTimeMinutes?: number
  id: number
  name: string
  createdAt: number
  dietId: number | null
}
export interface ShoppingItem {
  category?: string | null
  purchaseDayOfWeek?: number | null
  id: number
  shoppingListId: number
  name: string
  amount: string
  unit: Unit
  isBought: boolean
  isCustom: boolean
  isFresh: boolean
  dayOfWeek: number | null
}
export interface ShoppingDay {
  purchaseDayOfWeek?: number | null
  isBought: boolean
  id: number
  shoppingListItemId: number
  dayOfWeek: number
  amount: string
  unit: Unit
}
export interface Snapshot {
  schemaVersion: number
  diets: Diet[]
  meals: Meal[]
  courses: Course[]
  foods: Food[]
  lists: ShoppingList[]
  items: ShoppingItem[]
  days: ShoppingDay[]
}
export interface Workspace {
  version: number
  snapshot: Snapshot
}
export interface Profile {
  userId: string
  username: string
  dailyCalorieGoal: number
  updatedAt: number
}
export interface DriveStatus {
  available: boolean
  connected: boolean
  email: string | null
}
export interface Backup {
  id: string
  name: string
  createdTime: string
  size: string | null
}
export interface AdminUser {
  id: string
  email: string
  username: string
  role: string
  enabled: boolean
  googleLinked: boolean
  createdAt: number
  emailVerified?: boolean
  enrollmentStatus?: string
  permissions?: import('./access').Permission[]
  version?: number
  reviewReason?: string | null
}
export interface UserPage {
  users: AdminUser[]
  total: number
  page: number
  pages: number
}
export interface Overview {
  users: number
  activeUsers: number
  googleUsers: number
  workspaces: number
  driveConnections: number
  uptimeSeconds: number
}
export interface Audit {
  id: number
  actorId: string
  action: string
  targetId: string
  createdAt: number
}
export interface VersionPolicy {
  minSupportedVersionCode: number
  deprecatedVersionCode: number
  latestVersionCode: number
  latestVersionName: string
  minSupportedMajor: number
  updateUrl: string
}
export interface ManagedPolicy {
  policy: VersionPolicy
  revision: number
}

export interface Enrollment {
  account: Account
  history: { id: number; status: string; message: string; createdAt: number }[]
}
